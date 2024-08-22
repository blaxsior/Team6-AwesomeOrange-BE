package hyundai.softeer.orange.event.draw.entity;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;

@Table(name="event_participation_info")
@Getter
@Entity
public class EventParticipationInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Instant date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_user_id")
    private EventUser eventUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="draw_event_id")
    private DrawEvent drawEvent;

    public static EventParticipationInfo of(Instant date, EventUser eventUser, DrawEvent drawEvent) {
        EventParticipationInfo participationInfo = new EventParticipationInfo();
        participationInfo.date = date;
        participationInfo.eventUser = eventUser;
        participationInfo.drawEvent = drawEvent;
        return participationInfo;
    }
}
