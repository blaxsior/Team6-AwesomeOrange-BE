package hyundai.softeer.orange.eventuser.entity;

import hyundai.softeer.orange.comment.Comment;
import hyundai.softeer.orange.event.common.EventFrame;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Table(name="event_user")
@Getter
@Entity
public class EventUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column
    private String username;

    @Column
    private String phoneNumber;

    @Column
    private Integer score;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_frame_id")
    private EventFrame eventFrame;

    @OneToMany(mappedBy = "event_user")
    private List<Comment> commentList = new ArrayList<>();
}
