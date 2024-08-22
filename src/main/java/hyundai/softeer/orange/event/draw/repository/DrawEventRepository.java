package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DrawEventRepository extends JpaRepository<DrawEvent, Long> {
    @Query(value = "SELECT d FROM DrawEvent d WHERE d.eventMetadata.eventId = :eventId")
    Optional<DrawEvent> findByEventId(@Param("eventId") String eventId);

    // eventframeId로 eventmetadata의 drawevent를 찾는 fetch join 쿼리 (N+1 문제 방지)
    @Query(value = "SELECT d.* FROM draw_event d " +
            "JOIN event_metadata em ON d.event_metadata_id = em.id " +
            "JOIN event_frame ef ON em.event_frame_id = ef.id " +
            "WHERE ef.frame_id = :eventFrameId " +
            "LIMIT 1", nativeQuery = true)
    Optional<DrawEvent> findByEventFrameId(@Param("eventFrameId") String eventFrameId);

//    @Modifying // 수정 쿼리는 Modifying 필요
//    @Query(value = "UPDATE DrawEvent d SET d.isDrawn = true WHERE d.id = :id")
//    void updateIsDrawnById(@Param("id") Long id);
}
