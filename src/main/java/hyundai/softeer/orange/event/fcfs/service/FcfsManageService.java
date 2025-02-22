package hyundai.softeer.orange.event.fcfs.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsInfoDto;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsWinnerDto;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEvent;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEventWinningInfo;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventRepository;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventWinningInfoRepository;
import hyundai.softeer.orange.event.fcfs.util.FcfsUtil;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class FcfsManageService {

    private static final Logger log = LoggerFactory.getLogger(FcfsManageService.class);
    private final EventUserRepository eventUserRepository;
    private final FcfsEventRepository fcfsEventRepository;
    private final FcfsEventWinningInfoRepository fcfsEventWinningInfoRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Integer> numberRedisTemplate;
    private final RedisTemplate<String, Boolean> booleanRedisTemplate;

    // 오늘의 선착순 이벤트 정보(당첨자 수, 시작 시각)를 Redis에 배치
    @Transactional(readOnly = true)
    public void registerFcfsEvents() {
        List<FcfsEvent> events = fcfsEventRepository.findByStartTimeBetween(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS));
        events.forEach(this::prepareEventInfo);
        log.info("Today's FCFS events were registered in Redis");
    }

    // redis에 저장된 모든 선착순 이벤트의 당첨자 정보를 DB로 이관
    @Transactional
    public void registerWinners() {
        Set<String> fcfsKeys = stringRedisTemplate.keys("*:count");
        if (fcfsKeys == null || fcfsKeys.isEmpty()) {
            log.info("There are no FCFS events in yesterday");
            return;
        }

        // 당첨자 관련 정보 조합하여 Entity 생성
        log.info("keys for FCFS Events: {}", fcfsKeys);
        for(String key : fcfsKeys) {
            String fcfsEventId = key.replace(":count", "").replace("fcfs:", "");
            Set<String> userIds = stringRedisTemplate.opsForZSet().range(FcfsUtil.winnerFormatting(fcfsEventId), 0, -1);
            if(userIds == null || userIds.isEmpty()) {
                log.info("No winners in FCFS Event {}", fcfsEventId);
                continue;
            }

            FcfsEvent event = fcfsEventRepository.findById(Long.parseLong(fcfsEventId))
                    .orElseThrow(() -> new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND));

            List<EventUser> users = eventUserRepository.findAllByUserId(userIds.stream().toList());
            List<FcfsEventWinningInfo> winningInfos = users
                    .stream()
                    .map(user -> FcfsEventWinningInfo.of(event, user, getTimeFromScore(stringRedisTemplate.opsForZSet().score(FcfsUtil.winnerFormatting(fcfsEventId), user.getUserId()))))
                    .toList();

            log.info("Winners of FCFS event {} were registered in DB", fcfsEventId);
            fcfsEventWinningInfoRepository.saveAll(winningInfos);
            deleteEventInfo(fcfsEventId);
        }

        // PK를 간접적으로 보관하던 eventId 제거
        Set<String> eventIds = stringRedisTemplate.keys("*:eventId");
        if(eventIds != null && !eventIds.isEmpty()) {
            for(String eventId : eventIds) {
                stringRedisTemplate.delete(eventId);
            }
        }
        log.info("Registering winners of FCFS events in DB is completed");
    }

    // 특정 선착순 이벤트의 정보 조회
    public ResponseFcfsInfoDto getFcfsInfo(String eventId) {
        String key = getFcfsKeyFromEventId(eventId);

        String startTime = stringRedisTemplate.opsForValue().get(FcfsUtil.startTimeFormatting(key));
        // 선착순 이벤트가 존재하지 않는 경우
        if (startTime == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }

        Instant nowDateTime = Instant.now();
        Instant eventStartTime = Instant.parse(startTime);

        // 서버시간 < 이벤트시작시간 < 서버시간+3시간 -> countdown
        // 이벤트시작시간 < 서버시간 < 이벤트시작시간+7시간 -> progress
        // 그 외 -> waiting
        log.info("Checked FCFS event status: {}", key);
        if(nowDateTime.isBefore(eventStartTime) && nowDateTime.plus(ConstantUtil.FCFS_COUNTDOWN_HOUR, ChronoUnit.HOURS).isAfter(eventStartTime)) {
            return new ResponseFcfsInfoDto(eventStartTime, ConstantUtil.COUNTDOWN);
        } else if(eventStartTime.isBefore(nowDateTime) && eventStartTime.plus(ConstantUtil.FCFS_AVAILABLE_HOUR, ChronoUnit.HOURS).isAfter(nowDateTime)) {
            return new ResponseFcfsInfoDto(eventStartTime, ConstantUtil.PROGRESS);
        } else {
            return new ResponseFcfsInfoDto(eventStartTime, ConstantUtil.WAITING);
        }
    }

    // 특정 유저가 선착순 이벤트의 참여자인지 조회 (정답을 맞힌 경우 참여자로 간주)
    @Transactional(readOnly = true)
    public Boolean isParticipated(String eventId, String userId) {
        String key = getFcfsKeyFromEventId(eventId);
        if(!fcfsEventRepository.existsById(Long.parseLong(key))) {
            log.error("eventId {} 에 해당되는 선착순 이벤트를 DB에서 찾을 수 없음", eventId);
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(FcfsUtil.participantFormatting(key), userId));
    }

    // 특정 선착순 이벤트의 당첨자 조회 - 어드민에서 사용
    @Transactional(readOnly = true)
    public List<ResponseFcfsWinnerDto> getFcfsWinnersInfo(Long eventSequence) {
        log.info("Checked FCFS winners: {}", eventSequence);
        return fcfsEventWinningInfoRepository.findByFcfsEventId(eventSequence)
                .stream()
                .map(winningInfo -> ResponseFcfsWinnerDto.builder()
                        .name(winningInfo.getEventUser().getUserName())
                        .phoneNumber(winningInfo.getEventUser().getPhoneNumber())
                        .winningTime(winningInfo.getWinningTime())
                        .build())
                .toList();
    }

    private void prepareEventInfo(FcfsEvent event) {
        String key = event.getId().toString();
        stringRedisTemplate.opsForValue().set(FcfsUtil.eventIdFormatting(event.getEventMetaData().getEventId()), key);
        numberRedisTemplate.opsForValue().set(FcfsUtil.keyFormatting(key), event.getParticipantCount().intValue());
        booleanRedisTemplate.opsForValue().set(FcfsUtil.endFlagFormatting(key), false);
        stringRedisTemplate.opsForValue().set(FcfsUtil.startTimeFormatting(key), event.getStartTime().toString());

        // FIXME: 선착순 정답 생성 과정을 별도로 관리하는 것이 좋을 듯
        // 현재 정책 상 1~4 중 하나의 숫자를 선정하여 현재 선착순 이벤트의 정답에 저장
        int answer = new Random().nextInt(4) + 1;
        stringRedisTemplate.opsForValue().set(FcfsUtil.answerFormatting(key), String.valueOf(answer));
        log.info("Registered FCFS event: {}", key);
    }

    private void deleteEventInfo(String eventId) {
        stringRedisTemplate.delete(FcfsUtil.startTimeFormatting(eventId));
        stringRedisTemplate.delete(FcfsUtil.answerFormatting(eventId));
        stringRedisTemplate.delete(FcfsUtil.participantFormatting(eventId));
        stringRedisTemplate.delete(FcfsUtil.winnerFormatting(eventId));
        numberRedisTemplate.delete(FcfsUtil.keyFormatting(eventId));
        booleanRedisTemplate.delete(FcfsUtil.endFlagFormatting(eventId));
        log.info("Deleted Information of FCFS event: {}", eventId);
    }

    private Instant getTimeFromScore(Double score) {
        if(score == null) {
            log.error("score 값이 null");
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        long timeMillis = score.longValue();
        return Instant.ofEpochMilli(timeMillis);
    }

    private String getFcfsKeyFromEventId(String eventId) {
        String key = stringRedisTemplate.opsForValue().get(FcfsUtil.eventIdFormatting(eventId));
        if(key == null) {
            log.error("eventId {} 에 해당되는 key를 Redis 상에서 찾을 수 없음", eventId);
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        return key;
    }
}
