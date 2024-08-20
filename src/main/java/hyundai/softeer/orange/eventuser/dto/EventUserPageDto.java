package hyundai.softeer.orange.eventuser.dto;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class EventUserPageDto {
    private List<EventUserOnAdminDto> users;
    private int totalPage;
    private int number;
    private int size;

    public static EventUserPageDto from(Page<EventUser> userPage) {
        EventUserPageDto dto = new EventUserPageDto();
        dto.users = userPage.getContent().stream().map(EventUserOnAdminDto::from).toList();
        dto.totalPage = userPage.getTotalPages();
        dto.number = userPage.getNumber();
        dto.size = userPage.getSize();
        return dto;
    }
}
