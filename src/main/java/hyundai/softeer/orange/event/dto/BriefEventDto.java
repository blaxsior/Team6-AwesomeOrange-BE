package hyundai.softeer.orange.event.dto;

import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.dto.group.EventEditGroup;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class BriefEventDto {
    /**
     * HD000000_000 형식으로 구성된 id 값
     */
    String eventId;
    /**
     * 이벤트의 이름
     */
    private String name;

    /**
     * 이벤트 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 이벤트 종료 시간
     */
    private LocalDateTime endTime;

    /**
     * 이벤트의 타입
     */
    private EventType eventType;

    public static BriefEventDto of(String eventId, String name, LocalDateTime startTime, LocalDateTime endTime, EventType eventType) {
        BriefEventDto briefEventDto = new BriefEventDto();
        briefEventDto.eventId = eventId;
        briefEventDto.name = name;
        briefEventDto.startTime = startTime;
        briefEventDto.endTime = endTime;
        briefEventDto.eventType = eventType;
        return briefEventDto;
    }
}
