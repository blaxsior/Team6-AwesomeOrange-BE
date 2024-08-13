package hyundai.softeer.orange.comment.repository;

import hyundai.softeer.orange.comment.dto.ResponseCommentDto;
import hyundai.softeer.orange.comment.dto.WriteCommentCountDto;
import hyundai.softeer.orange.comment.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // DB 상에서 무작위로 추출된 n개의 긍정 기대평 목록을 조회하고 Dto로 반환, N+1 문제 방지 (JPQL)
    @Query("select new hyundai.softeer.orange.comment.dto.ResponseCommentDto(c.id, c.content, cu.userName, c.createdAt) " +
            "from Comment c " +
            "JOIN c.eventUser cu " +
            "where c.eventFrame.id = :eventFrameId and c.isPositive = true " +
            "order by function('RAND')")
    List<ResponseCommentDto> findRandomPositiveComments(Long eventFrameId, Pageable pageable);

    // 오늘 날짜 기준으로 이미 유저의 기대평이 등록되어 있는지 확인 (JPQL)
    @Query("SELECT (COUNT(c) > 0) FROM Comment c WHERE c.eventUser.id = :eventUserId AND FUNCTION('DATE', c.createdAt) = CURRENT_DATE")
    boolean existsByCreatedDateAndEventUser(@Param("eventUserId") Long eventUserId);

    @Query(value = "SELECT c.* FROM comment c " +
            "JOIN event_frame ef ON c.event_frame_id = ef.id " +
            "JOIN event_metadata e ON ef.id = e.event_frame_id " +
            "WHERE e.event_id = :eventId",
            countProjection = "c.id", // 어떤 값으로 count 셀건지 지정. 지정 안하면 count(c.*)가 되어 문제 발생.
            nativeQuery = true)
    Page<Comment> findAllByEventId(@Param("eventId") String eventId, Pageable pageable);

    // 이름 매핑 필요. 필드 이름과 직접 매핑.
    @Query(value = "SELECT c.event_user_id as eventUserId, COUNT(c.event_user_id) as count " +
            "FROM comment c " +
            "JOIN event_frame ef ON c.event_frame_id = ef.id " +
            "JOIN event_metadata e ON ef.id = e.event_frame_id " +
            "WHERE e.id = :eventRawId " +
            "GROUP BY c.event_user_id " , nativeQuery = true)
    List<WriteCommentCountDto> countPerEventUserByEventId(@Param("eventRawId") Long eventRawId);
}
