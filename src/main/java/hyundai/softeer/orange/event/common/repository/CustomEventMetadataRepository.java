package hyundai.softeer.orange.event.common.repository;

import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.dto.BriefEventDto;
import hyundai.softeer.orange.event.dto.EventSearchHintDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface CustomEventMetadataRepository {
    Page<BriefEventDto> findBriefsBySearch(String search, Set<EventType> eventTypes, Pageable pageRequest);
    List<EventSearchHintDto> findHintsBySearch(String search);
}
