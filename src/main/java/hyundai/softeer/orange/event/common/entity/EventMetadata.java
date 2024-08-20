package hyundai.softeer.orange.event.common.entity;

import hyundai.softeer.orange.event.common.enums.EventStatus;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "event_metadata")
@Entity
public class EventMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String eventId;

    @Column(length=40)
    private String name;

    @Column(length = 100)
    private String description;

    @Column
    private LocalDateTime startTime;

    @Column
    private LocalDateTime endTime;

    @Column
    private EventType eventType;

    @Column
    private String url;

    @Column
    private EventStatus status;

    public void updateName(String name) {
        this.name = name;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void updateEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public void updateUrl(String url) {
        this.url = url;
    }

    public boolean isEnded(LocalDateTime now) {
        return (this.status == EventStatus.COMPLETE) || endTime.isBefore(now);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="event_frame_id")
    private EventFrame eventFrame;

    // eventFrame을 거치지 않고 유저를 확인하기 위해 사용
    @Column(name = "event_frame_id", updatable = false, insertable = false)
    private Long eventFrameId;

    // 원래는 one-to-one 관계이지만, JPA 동작에 의해 강제로 EAGER FETCH로 처리돰 -> one-to-many 로 관리
    @OneToMany(mappedBy = "eventMetadata", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.LAZY)
    private final List<DrawEvent> drawEventList = new ArrayList<>();

    public void updateDrawEvent(DrawEvent drawEvent) {
        this.drawEventList.add(drawEvent);
    }

    public DrawEvent getDrawEvent() {
        if (drawEventList.isEmpty()) return null;
        return drawEventList.get(0);
    }

    @OneToMany(mappedBy = "eventMetaData", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private final List<FcfsEvent> fcfsEventList = new ArrayList<>();

    public void addFcfsEvents(List<FcfsEvent> fcfsEventList) {
        this.fcfsEventList.addAll(fcfsEventList);
    }

    public void addFcfsEvent(FcfsEvent fcfsEvent) {
        this.fcfsEventList.add(fcfsEvent);
    }
}
