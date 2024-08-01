package hyundai.softeer.orange.event.fcfs.entity;

import hyundai.softeer.orange.event.common.entity.EventMetadata;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Table(name="fcfs_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class FcfsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private Long participantCount;

    @Column
    private String prizeInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_metadata_id")
    private EventMetadata eventMetaData;

    @OneToMany(mappedBy = "fcfsEvent")
    private List<FcfsEventWinningInfo> infos = new ArrayList<>();

    public static FcfsEvent of(LocalDateTime startTime, LocalDateTime endTime, Long participantCount, String prizeInfo, EventMetadata eventMetadata) {
        FcfsEvent fcfsEvent = new FcfsEvent();
        fcfsEvent.startTime = startTime;
        fcfsEvent.endTime = endTime;
        fcfsEvent.participantCount = participantCount;
        fcfsEvent.prizeInfo = prizeInfo;
        fcfsEvent.eventMetadata = eventMetadata;
        return fcfsEvent;
    }
}
