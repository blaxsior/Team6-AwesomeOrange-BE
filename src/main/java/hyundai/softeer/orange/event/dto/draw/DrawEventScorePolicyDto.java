package hyundai.softeer.orange.event.dto.draw;

import hyundai.softeer.orange.event.draw.enums.DrawEventAction;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

/**
 * 추첨 이벤트에서 수행하는 행위에 대한 점수 정책을 정의하는 객체
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class DrawEventScorePolicyDto {
    /**
     * 점수 정책의 id. 서버 내부적으로 사용하는 값. 직접 설정 X
     */
    private Long id;

    /**
     * 유저의 점수를 증가시키는 행동
     */
    @NotNull
    private DrawEventAction action;

    /**
     * action 1회 수행 시 증가하는 점수
     */
    @NotNull
    @Positive
    private Integer score;
}
