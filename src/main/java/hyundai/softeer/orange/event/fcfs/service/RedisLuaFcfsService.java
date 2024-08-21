package hyundai.softeer.orange.event.fcfs.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.util.FcfsUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@RequiredArgsConstructor
@Primary
@Service
public class RedisLuaFcfsService implements FcfsService {

    private static final Logger log = LoggerFactory.getLogger(RedisLuaFcfsService.class);
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Integer> numberRedisTemplate;
    private final RedisTemplate<String, Boolean> booleanRedisTemplate;

    @Override
    public boolean participate(String eventId, String userId) {
        long startTimeMillis = System.currentTimeMillis();
        String key = stringRedisTemplate.opsForValue().get(FcfsUtil.eventIdFormatting(eventId));
        if(key == null) {
            log.error("eventId {} 에 해당되는 key를 Redis 상에서 찾을 수 없음", eventId);
            throw new FcfsEventException(ErrorCode.EVENT_NOT_FOUND);
        }
        Long eventSequence = Long.parseLong(key);

        // 이벤트 종료 여부 확인
        if (isEventEnded(key)) {
            stringRedisTemplate.opsForSet().add(FcfsUtil.participantFormatting(key), userId);
            log.info("이벤트가 이미 종료되어 아웃됨, 총 실행시간 {}", System.currentTimeMillis() - startTimeMillis);
            return false;
        }

        // 이미 이 이벤트에 참여했는지 확인
        if(isParticipated(key, userId)) {
            throw new FcfsEventException(ErrorCode.ALREADY_PARTICIPATED);
        }

        // 잘못된 이벤트 참여 시간
        String startTime = stringRedisTemplate.opsForValue().get(FcfsUtil.startTimeFormatting(key));
        if(startTime == null) {
            log.error("eventId {}의 시작시간을 Redis 상에서 찾을 수 없음", eventId);
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }

        if (LocalDateTime.now().isBefore(LocalDateTime.parse(startTime))){
            throw new FcfsEventException(ErrorCode.INVALID_EVENT_TIME);
        }

        long timeMillis = System.currentTimeMillis();
        String script = "local count = redis.call('zcard', KEYS[1]) " +
                "if count < tonumber(ARGV[1]) then " +
                "    redis.call('zadd', KEYS[1], ARGV[2], ARGV[3]) " +
                "    return redis.call('zcard', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
        long timestamp = System.currentTimeMillis();
        Long result = stringRedisTemplate.execute(
                RedisScript.of(script, Long.class),
                Collections.singletonList(FcfsUtil.winnerFormatting(key)),
                String.valueOf(numberRedisTemplate.opsForValue().get(FcfsUtil.keyFormatting(key))),
                String.valueOf(timestamp),
                userId
        );
        log.info("LuaScript 소요시간: {}", System.currentTimeMillis() - timeMillis);

        if(result == null || result <= 0) {
            log.info("Event Finished: {},", stringRedisTemplate.opsForZSet().zCard(FcfsUtil.winnerFormatting(key)));
            stringRedisTemplate.opsForSet().add(FcfsUtil.participantFormatting(key), userId);
            endEvent(key);
            log.info("LuaScript 진입은 했으나 이벤트가 종료되어 아웃 {}", System.currentTimeMillis() - startTimeMillis);
            return false;
        }

        stringRedisTemplate.opsForZSet().add(FcfsUtil.winnerFormatting(key), userId, System.currentTimeMillis());
        stringRedisTemplate.opsForSet().add(FcfsUtil.participantFormatting(key), userId);
        log.info("Participating Success: {}, User ID: {}, Timestamp: {}", eventSequence, userId, timestamp);
        log.info("성공까지 걸린 시간: {}", System.currentTimeMillis() - startTimeMillis);
        return true;
    }

    public boolean isParticipated(String key, String userId) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(FcfsUtil.participantFormatting(key), userId));
    }

    private boolean isEventEnded(String key) {
        return Boolean.TRUE.equals(booleanRedisTemplate.opsForValue().get(FcfsUtil.endFlagFormatting(key)));
    }

    private void endEvent(String key) {
        booleanRedisTemplate.opsForValue().set(FcfsUtil.endFlagFormatting(key), true);
    }
}
