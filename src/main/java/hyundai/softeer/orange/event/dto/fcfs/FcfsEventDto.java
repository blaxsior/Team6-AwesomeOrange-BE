package hyundai.softeer.orange.event.dto.fcfs;

import hyundai.softeer.orange.event.dto.validator.FcfsEventDtoTimeValidation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

/**
 * 선착순 이벤트를 표현하는 객체
 */
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
@Getter
@FcfsEventDtoTimeValidation
public class FcfsEventDto {
    /**
     * Fcfs 이벤트의 id. 서버 db 측에서 사용하기 위한 값. 직접 설정 X
     */
    private Long id;

    /**
     * 시작 시간
     */
    @NotNull
    private Instant startTime;

    /**
     * 종료 시간
     */
    @NotNull
    private Instant endTime;

    /**
     * 당첨 인원
     */
    @NotNull
    @Positive
    private Long participantCount;

    /**
     * 상품 관련된 정보를 저장하는 영역
     */
    @NotBlank
    private String prizeInfo;
}