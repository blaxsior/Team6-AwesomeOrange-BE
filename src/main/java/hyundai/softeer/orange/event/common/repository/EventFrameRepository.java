package hyundai.softeer.orange.event.common.repository;

import hyundai.softeer.orange.event.common.entity.EventFrame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFrameRepository extends JpaRepository<EventFrame, Long> {
    @Query("SELECT ef.frameId FROM EventFrame ef")
    List<String> findAllFrameIds();

    Optional<EventFrame> findByFrameId(String frameId);

    boolean existsByFrameId(String frameId);

    @Query("SELECT ef.frameId FROM EventFrame ef WHERE ef.frameId LIKE %:search%")
    List<String> findAllFrameIdsWithLike(@Param("search") String search);
}
