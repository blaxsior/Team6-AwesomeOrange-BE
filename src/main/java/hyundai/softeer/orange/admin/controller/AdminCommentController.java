package hyundai.softeer.orange.admin.controller;
import hyundai.softeer.orange.comment.dto.CommentsPageDto;
import hyundai.softeer.orange.comment.dto.DeleteCommentsDto;
import hyundai.softeer.orange.comment.service.CommentService;
import hyundai.softeer.orange.core.auth.list.AdminAuth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin comment", description = "어드민 페이지에서 댓글 관리에 사용되는 api")
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
@RestController
@AdminAuth
public class AdminCommentController {
    private final CommentService commentService;

    /**
     * @param eventId 댓글을 검색할 이벤트 id
     * @param page 댓글 페이지. default = 0
     * @param size 한번에 읽어 올 댓글 개수. default = 10
     * @return 대상 이벤트에 대해 검색된 댓글 목록
     */
    @Operation(summary = "관리자가 이벤트에 대한 댓글 목록 조회", description = "이벤트에 대한 댓글 목록을 조회한다.", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트에 대한 댓글 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<CommentsPageDto> findEventComments(
            @RequestParam String eventId,
            @RequestParam(required=false) String search,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size
    ) {
         var comments = commentService.searchComments(eventId, search, page, size);
         return ResponseEntity.ok(comments);
    }

    @Operation(summary = "관리자가 댓글 목록 삭제", description = "관리자가 이벤트에 대한 댓글 목록을 삭제한다.", responses = {
            @ApiResponse(responseCode = "200", description = "댓글 삭제 성공")
    })
    @DeleteMapping
    public ResponseEntity<Void> deleteEventComments(@Valid @RequestBody DeleteCommentsDto dto) {
        commentService.deleteComments(dto.getCommentIds());
        return ResponseEntity.ok().build();
    }
}
