package hyundai.softeer.orange.event.fcfs.scheduler;

import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.fcfs.service.FcfsManageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class FcfsScheduler {

    private static final Logger log = LoggerFactory.getLogger(FcfsScheduler.class);
    // 분산 환경에서 메서드가 여러 번 실행되는 것을 방지하기 위해 분산 락 도입
    private final RedissonClient redissonClient;
    private final FcfsManageService fcfsManageService;

    // 매일 자정 1분마다 실행되며, 오늘의 선착순 이벤트에 대한 정보를 DB에서 Redis로 이동시킨다.
    @Scheduled(cron = "0 1 0 * * *")
    public void registerFcfsEvents() {
        log.info("Move the information of FCFS Events from DB to Redis");
        RLock lock = redissonClient.getLock(ConstantUtil.DB_TO_REDIS_LOCK);
        try {
            // 5분동안 락 점유
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    fcfsManageService.registerFcfsEvents();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // 매일 자정마다 실행되며, 선착순 이벤트 당첨자들을 Redis에서 DB로 이동시킨다.
    @Scheduled(cron = "0 0 0 * * *")
    public void registerWinners() {
        log.info("Move the result of FCFS Events from Redis to DB");
        RLock lock = redissonClient.getLock(ConstantUtil.REDIS_TO_DB_LOCK);
        try {
            // 5분동안 락 점유
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    fcfsManageService.registerWinners();
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // FIXME: 빌드 직후 오늘의 선착순 이벤트에 대한 정보를 DB에서 Redis로 이동시킨다. (추후 삭제예정)
    @PostConstruct
    public void init() {
        registerFcfsEvents();
    }
}
