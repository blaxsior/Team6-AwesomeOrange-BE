package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.event.draw.dto.EventParticipateCountDto;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDateDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.entity.EventParticipationInfo;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventParticipationInfoRepository extends JpaRepository<EventParticipationInfo, Long> {
    @Query(value = "SELECT event_user_id as eventUserId, COUNT(event_user_id) as count " +
            "FROM event_participation_info " +
            "WHERE draw_event_id = :eventRawId " +
            "GROUP BY event_user_id", nativeQuery = true)
    List<EventParticipateCountDto> countPerEventUserByEventId(Long eventRawId);

    @Query(value = "SELECT info.date as date FROM event_participation_info info " +
            "JOIN event_user e ON info.event_user_id =  e.id " +
            "WHERE e.user_id = :eventUserId " +
            "AND info.draw_event_id = :drawEventId", nativeQuery = true)
    List<EventParticipationDateDto> findByEventUserId(@Param("eventUserId") String eventUserId, @Param("drawEventId") Long drawEventId);

    boolean existsByEventUserAndDrawEventAndDateBetween(EventUser eventUser, DrawEvent drawEvent, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
