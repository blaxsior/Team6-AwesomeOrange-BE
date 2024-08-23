package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.event.draw.entity.DrawEventWinningInfo;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DrawEventWinningInfoRepository extends JpaRepository<DrawEventWinningInfo, Long>, CustomDrawEventWinningInfoRepository {
    @EntityGraph(attributePaths = {"eventUser"})
    List<DrawEventWinningInfo> findAllByDrawEventId(Long eventId);
}
