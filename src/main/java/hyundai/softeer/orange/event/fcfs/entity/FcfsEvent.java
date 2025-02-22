package hyundai.softeer.orange.event.fcfs.entity;

import hyundai.softeer.orange.event.common.entity.EventMetadata;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Table(name="fcfs_event")
@Getter
@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class FcfsEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private Instant startTime;

    @Column
    private Instant endTime;

    @Column
    private Long participantCount;

    @Column
    private String prizeInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_metadata_id")
    private EventMetadata eventMetaData;

    @OneToMany(mappedBy = "fcfsEvent")
    private final List<FcfsEventWinningInfo> infos = new ArrayList<>();

    public void updateStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public void updateEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public void updateParticipantCount(Long participantCount) {
        this.participantCount = participantCount;
    }

    public void updatePrizeInfo(String prizeInfo) {
        this.prizeInfo = prizeInfo;
    }

    public static FcfsEvent of(Instant startTime, Instant endTime, Long participantCount, String prizeInfo, EventMetadata eventMetadata) {
        FcfsEvent fcfsEvent = new FcfsEvent();
        fcfsEvent.startTime = startTime;
        fcfsEvent.endTime = endTime;
        fcfsEvent.participantCount = participantCount;
        fcfsEvent.prizeInfo = prizeInfo;
        fcfsEvent.eventMetaData = eventMetadata;
        return fcfsEvent;
    }
}
