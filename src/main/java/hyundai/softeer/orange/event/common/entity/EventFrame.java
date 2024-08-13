package hyundai.softeer.orange.event.common.entity;

import hyundai.softeer.orange.comment.entity.Comment;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name="event_frame")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class EventFrame {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 클라이언트가 PathVariable로 넘겨줄 값 (ex: the-new-ioniq5)
    @Column(unique = true, nullable = false)
    private String frameId;

    // 어드민에서 노출될 이름 (ex: 2024 하반기 신차 출시 이벤트)
    @Column(unique = true, nullable = false)
    private String name;

    @OneToMany(mappedBy="eventFrame")
    private List<EventMetadata> eventMetadataList = new ArrayList<>();

    @OneToMany(mappedBy="eventFrame")
    private List<EventUser> eventUserList = new ArrayList<>();

    @OneToMany(mappedBy="eventFrame")
    private List<Comment> commentList = new ArrayList<>();

    public static EventFrame of(String frame, String name) {
        EventFrame eventFrame = new EventFrame();
        eventFrame.frameId = frame;
        eventFrame.name = name;
        return eventFrame;
    }
}
