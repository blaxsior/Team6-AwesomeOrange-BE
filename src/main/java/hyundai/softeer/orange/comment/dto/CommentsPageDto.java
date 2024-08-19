package hyundai.softeer.orange.comment.dto;

import hyundai.softeer.orange.comment.entity.Comment;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class CommentsPageDto {
    /**
     * 댓글 목록
     */
    private List<ResponseCommentDto> comments;
    /**
     * 총 페이지 수
     */
    private int totalPages;

    /**
     * 현재 페이지 번호
     */
    private int number;

    /**
     * 페이지 크기
     */
    private int size;

    public static CommentsPageDto from(Page<Comment> page) {
        CommentsPageDto dto = new CommentsPageDto();
        dto.totalPages = page.getTotalPages();
        dto.number = page.getNumber();
        dto.size = page.getSize();
        dto.comments = page.getContent().stream().map(ResponseCommentDto::from).toList();
        return dto;
    }
}
