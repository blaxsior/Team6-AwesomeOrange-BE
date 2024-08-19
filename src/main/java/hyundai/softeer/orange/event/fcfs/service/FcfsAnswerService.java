package hyundai.softeer.orange.event.fcfs.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.util.FcfsUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class FcfsAnswerService {

    private final StringRedisTemplate stringRedisTemplate;

    public boolean judgeAnswer(String eventId, String answer) {
        // eventId로부터 FCFS의 key를 가져옴
        String key = stringRedisTemplate.opsForValue().get(eventId);
        if(key == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }

        // 잘못된 이벤트 참여 시간
        String startTime = stringRedisTemplate.opsForValue().get(FcfsUtil.startTimeFormatting(key));
        if(startTime == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        if (LocalDateTime.now().isBefore(LocalDateTime.parse(startTime))){
            throw new FcfsEventException(ErrorCode.INVALID_EVENT_TIME);
        }

        // 정답 비교
        String correctAnswer = stringRedisTemplate.opsForValue().get(FcfsUtil.answerFormatting(key));
        if (correctAnswer == null) {
            throw new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND);
        }
        return correctAnswer.trim().equals(answer.trim()); // 정답 비교 시 공백을 제거
    }
}
