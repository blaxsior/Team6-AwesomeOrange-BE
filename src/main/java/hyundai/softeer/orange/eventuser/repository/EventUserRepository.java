package hyundai.softeer.orange.eventuser.repository;

import hyundai.softeer.orange.eventuser.dto.EventUserScoreDto;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventUserRepository extends JpaRepository<EventUser, Long>, CustomEventUserRepository {
    Optional<EventUser> findByUserNameAndPhoneNumberAndEventFrameFrameId(String userName, String phoneNumber, String eventFrameId);

    boolean existsByPhoneNumberAndEventFrameFrameId(String phoneNumber, String frameId);

    Optional<EventUser> findByUserId(String userId);

    @Query("SELECT eu FROM EventUser eu WHERE eu.userId IN :userIds")
    List<EventUser> findAllByUserId(@Param("userIds") List<String> userIds);

    @Query("SELECT eu FROM EventUser eu join fetch eu.eventFrame WHERE eu.userName LIKE %:search%")
    Page<EventUser> findBySearch(@Param("search") String search, Pageable pageable);

    boolean existsByPhoneNumber(String phoneNumber);

    @Query(value = "SELECT u.id as userId, u.score as score FROM event_user u " +
            "JOIN event_frame ef ON ef.id = u.event_frame_id " +
            "JOIN event_metadata e ON e.event_frame_id = ef.id " +
            "WHERE e.id = :rawEventId", nativeQuery = true)
    List<EventUserScoreDto> findAllUserScoreByDrawEventId(@Param("rawEventId") long rawEventId);
}
