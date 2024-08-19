package hyundai.softeer.orange.admin.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminDto {
    private Long id;
    private String nickname;

    public static AdminDto of(Long id, String nickname) {
        AdminDto adminDto = new AdminDto();
        adminDto.id = id;
        adminDto.nickname = nickname;
        return adminDto;
    }
}
