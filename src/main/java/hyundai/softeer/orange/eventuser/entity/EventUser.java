package hyundai.softeer.orange.eventuser.entity;

import hyundai.softeer.orange.comment.entity.Comment;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.draw.entity.DrawEventWinningInfo;
import hyundai.softeer.orange.event.draw.entity.EventParticipationInfo;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEventWinningInfo;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Table(name="event_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
public class EventUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String userName;

    @Column
    private String phoneNumber;

    @Column(unique = true, nullable = false)
    private String userId;

    @Column
    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_frame_id")
    private EventFrame eventFrame;

    // eventFrame을 거치지 않고 이벤트를 매칭하기 위해 사용
    @Column(name = "event_frame_id", updatable = false, insertable = false)
    private Long eventFrameId;

    @OneToMany(mappedBy = "eventUser")
    private List<Comment> commentList = new ArrayList<>();

    @OneToMany(mappedBy = "eventUser")
    private List<EventParticipationInfo> participationInfoList  = new ArrayList<>();

    @OneToMany(mappedBy = "eventUser")
    private List<DrawEventWinningInfo> drawEventWinningInfoList = new ArrayList<>();

    @OneToMany(mappedBy = "eventUser")
    private List<FcfsEventWinningInfo> fcfsEventWinningInfoList = new ArrayList<>();

    public static EventUser of(String userName, String phoneNumber, EventFrame eventFrame, String uuid) {
        EventUser eventUser = new EventUser();
        eventUser.userName = userName;
        eventUser.phoneNumber = phoneNumber;
        eventUser.userId = uuid;
        eventUser.score = 0;
        eventUser.eventFrame = eventFrame;
        return eventUser;
    }
}
