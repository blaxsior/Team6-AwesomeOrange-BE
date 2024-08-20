package hyundai.softeer.orange.event.dto;

import hyundai.softeer.orange.event.dto.validator.EventDtoTimeValidation;
import hyundai.softeer.orange.event.dto.draw.DrawEventDto;
import hyundai.softeer.orange.event.dto.fcfs.FcfsEventDto;
import hyundai.softeer.orange.event.dto.group.EventEditGroup;
import hyundai.softeer.orange.event.common.enums.EventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 이벤트 정보를 포현하는 객체
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@EventDtoTimeValidation // 시작 시간이 끝 시간보다 크다는 것을 보장
public class EventDto {
    /**
     * HD000000_000 형식으로 구성된 id 값. 직접 설정할 필요 없음.
     */
    @NotNull(groups = {EventEditGroup.class})
    private String eventId;

    /**
     * 이벤트의 이름
     */
    @Size(min = 1, max = 40)
    @NotNull
    private String name;

    /**
     * 이벤트에 대한 설명
     */
    @Size(min = 1, max = 100)
    @NotNull
    private String description;

    /**
     * 이벤트 시작 시간
     */
    @NotNull
    private LocalDateTime startTime;

    /**
     * 이벤트 종료 시간
     */
    @NotNull
    private LocalDateTime endTime;

    /**
     * 이벤트 페이지의 url
     */
    @NotBlank
    private String url;


    /**
     * 이벤트의 타입
     */
    @NotNull
    private EventType eventType;

    /**
     * 이벤트 프레임 id
     */
    @NotNull
    private String eventFrameId;

    /**
     * fcfs 이벤트 내용을 정의하는 부분. eventType = fcfs일 때 설정
     */
    @Setter
    @Valid
    private List<FcfsEventDto> fcfs;

    /**
     * draw 이벤트 내용을 정의하는 부분. eventType = draw일 때 설정
     */
    @Setter
    @Valid
    private DrawEventDto draw;

}