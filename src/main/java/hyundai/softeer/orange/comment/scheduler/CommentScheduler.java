package hyundai.softeer.orange.comment.scheduler;

import hyundai.softeer.orange.comment.service.CommentService;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CommentScheduler {

    private final CommentService commentService;
    private final EventFrameRepository eventFrameRepository;

    // 스케줄러에 의해 일정 시간마다 캐싱된 긍정 기대평 목록을 초기화한다.
    @Scheduled(fixedRate = ConstantUtil.SCHEDULED_TIME) // 2시간마다 실행
    private void clearCache() {
        List<Long> eventFrameIds = eventFrameRepository.findAllIds();
        eventFrameIds.forEach(commentService::getComments);
    }
}
