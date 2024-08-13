package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.event.draw.dto.EventParticipateCountDto;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDateDto;
import hyundai.softeer.orange.event.draw.entity.EventParticipationInfo;
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


    // boolean / exists를 시도했으나 실패. 개발이 어느 정도 진행된 이후 처리할 예정.
    @Query(value = "SELECT COUNT(*) "+
            "FROM event_participation_info info " +
            "JOIN event_user e ON info.event_user_id = e.id " +
            "WHERE e.user_id = :eventUserId " +
            "AND info.draw_event_id = :drawEventId " +
            "AND info.date BETWEEN :from AND :to " +
            "limit 1", nativeQuery = true)
    Long count1ByUserId(@Param("eventUserId") String eventUserId, @Param("drawEventId") Long drawEventId, @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
