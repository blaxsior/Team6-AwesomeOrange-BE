package hyundai.softeer.orange.event.common.repository;

import hyundai.softeer.orange.event.common.entity.EventMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventMetadataRepository extends JpaRepository<EventMetadata, Long>, CustomEventMetadataRepository {
    Optional<EventMetadata> findFirstByEventId(String eventId);

    List<EventMetadata> findByEventIdIn(List<String> eventIds);

    @Query("SELECT e from EventMetadata e join fetch e.eventFrame WHERE e.eventId = :eventId")
    Optional<EventMetadata> findByEventIdWithEventFrame(@Param("eventId") String eventId);
}
