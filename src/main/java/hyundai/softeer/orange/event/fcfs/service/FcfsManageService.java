package hyundai.softeer.orange.event.fcfs.service;

import hyundai.softeer.orange.common.ErrorCode;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class FcfsManageService {

    private final EventUserRepository eventUserRepository;
    private final FcfsEventRepository fcfsEventRepository;
    private final FcfsEventWinningInfoRepository fcfsEventWinningInfoRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Integer> numberRedisTemplate;
    private final RedisTemplate<String, Boolean> booleanRedisTemplate;

    // 오늘의 선착순 이벤트 정보(당첨자 수, 시작 시각)를 Redis에 배치
    @Transactional(readOnly = true)
    public void registerFcfsEvents() {
        List<FcfsEvent> events = fcfsEventRepository.findByStartTimeBetween(LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        events.forEach(this::prepareEventInfo);
    }

    // redis에 저장된 모든 선착순 이벤트의 당첨자 정보를 DB로 이관
    @Transactional
    public void registerWinners() {
        Set<String> fcfsIds = stringRedisTemplate.keys("*:fcfs");
        if (fcfsIds == null || fcfsIds.isEmpty()) {
            return;
        }

        for(String fcfsId : fcfsIds) {
            String eventId = fcfsId.replace(":fcfs", "");
            Set<String> userIds = stringRedisTemplate.opsForZSet().range(FcfsUtil.winnerFormatting(eventId), 0, -1);
            if(userIds == null || userIds.isEmpty()) {
                return;
            }

            FcfsEvent event = fcfsEventRepository.findById(Long.parseLong(eventId))
                    .orElseThrow(() -> new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND));

            List<EventUser> users = eventUserRepository.findAllByUserId(userIds.stream().toList());

            List<FcfsEventWinningInfo> winningInfos = users
                    .stream()
                    .map(user -> FcfsEventWinningInfo.of(event, user))
                    .toList();

            fcfsEventWinningInfoRepository.saveAll(winningInfos);
            deleteEventInfo(eventId);
        }
    }

    // 특정 선착순 이벤트의 당첨자 조회 - 어드민에서 사용
    @Transactional(readOnly = true)
    public List<ResponseFcfsWinnerDto> getFcfsWinnersInfo(Long eventSequence) {
        return fcfsEventWinningInfoRepository.findByFcfsEventId(eventSequence)
                .stream()
                .map(winningInfo -> ResponseFcfsWinnerDto.builder()
                        .name(winningInfo.getEventUser().getUserName())
                        .phoneNumber(winningInfo.getEventUser().getPhoneNumber())
                        .build())
                .toList();
    }

    private void prepareEventInfo(FcfsEvent event) {
        numberRedisTemplate.opsForValue().set(FcfsUtil.keyFormatting(event.getId().toString()), event.getParticipantCount().intValue());
        booleanRedisTemplate.opsForValue().set(FcfsUtil.endFlagFormatting(event.getId().toString()), false);
        stringRedisTemplate.opsForValue().set(FcfsUtil.startTimeFormatting(event.getId().toString()), event.getStartTime().toString());

        // FIXME: 선착순 정답 생성 과정을 별도로 관리하는 것이 좋을 듯
        // 현재 정책 상 1~4 중 하나의 숫자를 선정하여 현재 선착순 이벤트의 정답에 저장
        int answer = new Random().nextInt(4) + 1;
        stringRedisTemplate.opsForValue().set(FcfsUtil.answerFormatting(event.getId().toString()), String.valueOf(answer));
    }

    public void deleteEventInfo(String eventId) {
        stringRedisTemplate.delete(FcfsUtil.startTimeFormatting(eventId));
        stringRedisTemplate.delete(FcfsUtil.winnerFormatting(eventId));
        numberRedisTemplate.delete(FcfsUtil.keyFormatting(eventId));
        booleanRedisTemplate.delete(FcfsUtil.endFlagFormatting(eventId));
    }
}
