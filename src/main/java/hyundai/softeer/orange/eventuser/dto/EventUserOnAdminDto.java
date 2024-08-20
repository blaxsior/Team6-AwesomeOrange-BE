package hyundai.softeer.orange.eventuser.dto;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import lombok.Getter;

@Getter
public class EventUserOnAdminDto {
    private String userName;
    private String phoneNumber;
    private String frameId;

    public static EventUserOnAdminDto from(EventUser eventUser) {
        EventUserOnAdminDto dto = new EventUserOnAdminDto();
        dto.userName = eventUser.getUserName();
        dto.phoneNumber = eventUser.getPhoneNumber();
        dto.frameId = eventUser.getEventFrame().getFrameId();
        return dto;
    }
}
