package hyundai.softeer.orange.comment.dto;

import hyundai.softeer.orange.comment.entity.Comment;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Builder
@Getter
public class ResponseCommentDto {

    private Long id;
    private String content;
    private String userName;
    private Instant createdAt;

    // CommentRepository에서 Projection으로 ResponseCommentDto를 생성할 때 사용, 추후 더 나은 방법으로 수정 필요
    public ResponseCommentDto(Long id, String content, String userName, Instant createdAt) {
        this.id = id;
        this.content = content;
        this.userName = userName;
        this.createdAt = createdAt;
    }

    public static ResponseCommentDto from(Comment comment) {
        return ResponseCommentDto.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .userName(comment.getEventUser().getUserName())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
