package hyundai.softeer.orange.event.draw.entity;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table(name="draw_event_winning_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class DrawEventWinningInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Long ranking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "draw_event_id")
    private DrawEvent drawEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_user_id")
    private EventUser eventUser;

    public static DrawEventWinningInfo of(Long ranking, DrawEvent drawEvent, EventUser eventUser) {
        DrawEventWinningInfo drawEventWinningInfo = new DrawEventWinningInfo();
        drawEventWinningInfo.ranking = ranking;
        drawEventWinningInfo.drawEvent = drawEvent;
        drawEventWinningInfo.eventUser = eventUser;
        return drawEventWinningInfo;
    }
}
