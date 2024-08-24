package hyundai.softeer.orange.eventuser.repository;

import hyundai.softeer.orange.eventuser.dto.EventUserScoreDto;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomEventUserRepository {
    void updateScoreMany(List<EventUserScoreDto> dto);
    Page<EventUser> findBySearch(String search, String field, Pageable pageable);
}
