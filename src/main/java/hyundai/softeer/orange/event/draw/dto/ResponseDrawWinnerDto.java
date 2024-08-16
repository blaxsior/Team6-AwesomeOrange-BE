package hyundai.softeer.orange.event.draw.dto;

import hyundai.softeer.orange.event.draw.entity.DrawEventWinningInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResponseDrawWinnerDto {
    private Long ranking;
    private String name;
    private String phoneNumber;

    public ResponseDrawWinnerDto(DrawEventWinningInfo drawEventWinningInfo) {
        this.ranking = drawEventWinningInfo.getRanking();
        this.name = drawEventWinningInfo.getEventUser().getUserName();
        this.phoneNumber = drawEventWinningInfo.getEventUser().getPhoneNumber();
    }
}
