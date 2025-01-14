package hyundai.softeer.orange.event.fcfs.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.util.FcfsUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisLockFcfsService implements FcfsService {

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Boolean> booleanRedisTemplate;
    private final RedissonClient redissonClient;
    private final RedisTemplate<String, Integer> numberRedisTemplate;

    @Override
    public boolean participate(String eventId, String userId) {
        String key = stringRedisTemplate.opsForValue().get(FcfsUtil.eventIdFormatting(eventId));
        if(key == null) {
            throw new FcfsEventException(ErrorCode.EVENT_NOT_FOUND);
        }
        Long eventSequence = Long.parseLong(key);

        // 이벤트 종료 여부 확인
        if (isEventEnded(key)) {
            stringRedisTemplate.opsForSet().add(FcfsUtil.participantFormatting(key), userId);
            return false;
        }

        // 이미 이 이벤트에 참여했는지 확인
        if(isParticipated(key, userId)) {
            throw new FcfsEventException(ErrorCode.ALREADY_PARTICIPATED);
        }

        // 잘못된 이벤트 참여 시간
        String startTime = stringRedisTemplate.opsForValue().get(FcfsUtil.startTimeFormatting(key));
        if(startTime == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }

        if (Instant.now().isBefore(Instant.parse(startTime))){
            throw new FcfsEventException(ErrorCode.INVALID_EVENT_TIME);
        }

        // Lock을 이용한 참여 처리
        final RLock lock = redissonClient.getLock("LOCK:" + eventSequence);
        try {
            boolean usingLock = lock.tryLock(1L, 3L, TimeUnit.SECONDS);
            if (!usingLock) {
                return false;
            }

            int quantity = availableCoupons(FcfsUtil.keyFormatting(key));
            if (quantity <= 0) {
                log.info("Event Finished: {},", stringRedisTemplate.opsForZSet().zCard(FcfsUtil.winnerFormatting(key)));
                endEvent(key);  // 이벤트 종료 플래그 설정
                return false;
            }

            numberRedisTemplate.opsForValue().decrement(FcfsUtil.keyFormatting(key));
            stringRedisTemplate.opsForZSet().add(FcfsUtil.winnerFormatting(key), userId, System.currentTimeMillis());
            stringRedisTemplate.opsForSet().add(FcfsUtil.participantFormatting(key), userId);
            log.info("Participating Success: {}, User ID: {}", eventSequence, userId);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private boolean isParticipated(String key, String userId){
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(FcfsUtil.participantFormatting(key), userId));
    }

    private Integer availableCoupons(String key) {
        Integer count = numberRedisTemplate.opsForValue().get(key);
        if (count == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        return count;
    }

    private boolean isEventEnded(String key) {
        return Boolean.TRUE.equals(booleanRedisTemplate.opsForValue().get(FcfsUtil.endFlagFormatting(key)));
    }

    private void endEvent(String key) {
        booleanRedisTemplate.opsForValue().set(FcfsUtil.endFlagFormatting(key), true);
    }
}
