package hyundai.softeer.orange.event.draw.dto;

import hyundai.softeer.orange.event.draw.enums.DrawEventStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawEventStatusDto {
    public String eventId;
    public DrawEventStatus status;

    public static DrawEventStatusDto of(String eventId, DrawEventStatus status) {
        DrawEventStatusDto dto = new DrawEventStatusDto();
        dto.eventId = eventId;
        dto.status = status;
        return dto;
    }
}
