package hyundai.softeer.orange.event.fcfs.entity;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Table(name="fcfs_event_winning_info")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class FcfsEventWinningInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fcfs_event_id")
    private FcfsEvent fcfsEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="event_user_id")
    private EventUser eventUser;

    private LocalDateTime winningTime;

    public static FcfsEventWinningInfo of(FcfsEvent fcfsEvent, EventUser eventUser, LocalDateTime winningTime) {
        FcfsEventWinningInfo fcfsEventWinningInfo = new FcfsEventWinningInfo();
        fcfsEventWinningInfo.fcfsEvent = fcfsEvent;
        fcfsEventWinningInfo.eventUser = eventUser;
        fcfsEventWinningInfo.winningTime = winningTime;
        return fcfsEventWinningInfo;
    }
}
