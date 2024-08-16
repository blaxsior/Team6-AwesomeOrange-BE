package hyundai.softeer.orange.event.fcfs.repository;

import hyundai.softeer.orange.event.fcfs.entity.FcfsEventWinningInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FcfsEventWinningInfoRepository extends JpaRepository<FcfsEventWinningInfo, Long> {

    // Fetch Join으로 eventUser 정보까지 한번에 가져와서 N+1 문제 방지하며, 당첨 시각 기준 오름차순 정렬
    @Query("select f from FcfsEventWinningInfo f join fetch f.eventUser where f.fcfsEvent.id = :eventSequence order by f.winningTime asc")
    List<FcfsEventWinningInfo> findByFcfsEventId(Long eventSequence);

    boolean existsByEventUserIdAndFcfsEventId(Long eventUserId, Long fcfsEventId);
}
