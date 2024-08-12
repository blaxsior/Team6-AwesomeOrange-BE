package hyundai.softeer.orange.event.common.repository;

import hyundai.softeer.orange.event.common.entity.EventFrame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventFrameRepository extends JpaRepository<EventFrame, Long> {
    Optional<EventFrame> findByName(String name);

    @Query("SELECT ef.frameId FROM EventFrame ef")
    List<String> findAllFrameIds();

    Optional<EventFrame> findByFrameId(String frameId);
}
