package hyundai.softeer.orange.comment.scheduler;

import hyundai.softeer.orange.comment.service.CommentService;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class CommentScheduler {

    // 분산 환경에서 메서드가 여러 번 실행되는 것을 방지하기 위해 분산 락 도입
    private final RedissonClient redissonClient;
    private final CommentService commentService;
    private final EventFrameRepository eventFrameRepository;

    // 스케줄러에 의해 일정 시간마다 캐싱된 긍정 기대평 목록을 초기화한다.
    @Scheduled(fixedRate = ConstantUtil.SCHEDULED_TIME) // 2시간마다 실행
    private void clearCache() throws InterruptedException {
        RLock lock = redissonClient.getLock(ConstantUtil.DB_TO_REDIS_LOCK);
        try {
            // 5분동안 락 점유
            if (lock.tryLock(0, 5, TimeUnit.MINUTES)) {
                try {
                    List<String> frameIds = eventFrameRepository.findAllFrameIds();
                    frameIds.forEach(commentService::getComments);
                } finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
