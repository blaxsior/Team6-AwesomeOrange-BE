package hyundai.softeer.orange.event.common.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.core.ParseUtil;
import hyundai.softeer.orange.event.common.EventConst;
import hyundai.softeer.orange.event.common.component.eventFieldMapper.EventFieldMapperMatcher;
import hyundai.softeer.orange.event.common.component.eventFieldMapper.mapper.EventFieldMapper;
import hyundai.softeer.orange.event.common.component.query.EventSearchQueryParser;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventStatus;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.common.repository.EventSpecification;
import hyundai.softeer.orange.event.component.EventKeyGenerator;
import hyundai.softeer.orange.event.dto.BriefEventDto;
import hyundai.softeer.orange.event.dto.BriefEventPageDto;
import hyundai.softeer.orange.event.dto.EventDto;
import hyundai.softeer.orange.event.dto.EventSearchHintDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 이벤트 전반에 대한 조회, 수정을 다루는 서비스. 구체적인 액션(추첨 등)은 구체적인 클래스에서 처리 요망
 */
@RequiredArgsConstructor
@Service
public class EventService {
    private static final Logger log = LoggerFactory.getLogger(EventService.class);
    private final EventFrameRepository efRepository;
    private final EventMetadataRepository emRepository;
    private final EventFieldMapperMatcher mapperMatcher;
    private final EventKeyGenerator keyGenerator;
    private final RedisTemplate<String, Object> eventDtoRedisTemplate;
    private final ParseUtil parseUtil;

    /**
     * 이벤트를 생성한다.
     * @param eventDto 이벤트 dto
     */
    @Transactional
    public void createEvent(EventDto eventDto) {
        // 1. eventframe을 찾는다. 없으면 작업이 의미 X
        Optional<EventFrame> frameOpt = efRepository.findByFrameId(eventDto.getEventFrameId());
        EventFrame frame = frameOpt
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_FRAME_NOT_FOUND));

        String eventKey = keyGenerator.generate();

        // 2. 이벤트 메타데이터 객체를 생성한다.
        EventMetadata eventMetadata = EventMetadata.builder()
                .eventId(eventKey)
                .name(eventDto.getName())
                .description(eventDto.getDescription())
                .startTime(eventDto.getStartTime())
                .endTime(eventDto.getEndTime())
                .url(eventDto.getUrl())
                .eventType(eventDto.getEventType())
                .status(EventStatus.READY) // 아직 시작 안함.
                .eventFrame(frame)
                .build();

        EventType type = eventDto.getEventType();
        EventFieldMapper mapper = mapperMatcher.getMapper(type);
        if(mapper == null) throw new EventException(ErrorCode.INVALID_EVENT_TYPE);

        mapper.fetchToEventEntity(eventMetadata, eventDto);
        log.info("Created Event: {}", eventKey);
        emRepository.save(eventMetadata);
    }

    /**
     * 이벤트를 임시 저장한다.
     * @param adminId 이벤트를 임시 저장하는 관리자의 id
     * @param eventDto 임시 저장하는 이벤트 정보
     */
    public void saveTempEvent(Long adminId, EventDto eventDto) {
        String key = EventConst.ADMIN_TEMP(adminId);
        // 24시간 동안 유지.
        eventDtoRedisTemplate.opsForValue().set(key, eventDto, EventConst.TEMP_EVENT_DURATION_HOUR, TimeUnit.HOURS);
        log.info("Saved temp event by {}", adminId);
    }

    /**
     * 임시 저장 된 이벤트를 가져온다.
     * @param adminId 이벤트를 임시 저장한 관리자의 id
     * @return 임시 저장 된 이벤트 정보
     */
    public EventDto getTempEvent(Long adminId) {
        String key = EventConst.ADMIN_TEMP(adminId);
        String dto = (String) eventDtoRedisTemplate.opsForValue().get(key);

        log.info("Fetched temp event by {}", adminId);
        EventDto eventDto = parseUtil.parse(dto, EventDto.class);
        if(eventDto == null) throw new EventException(ErrorCode.TEMP_EVENT_NOT_FOUND);

        return eventDto;
    }

    /**
     * 임시 저장 된 이벤트를 제거한다.
     * @param adminId 이벤트를 임시 저장한 관리자의 id
     */
    public void clearTempEvent(Long adminId) {
        String key = EventConst.ADMIN_TEMP(adminId);
        eventDtoRedisTemplate.delete(key);
        log.info("Cleared temp event by {}", adminId);
    }

    /**
     * 이벤트를 수정한다.
     * @param eventDto 수정 데이터가 담긴 이벤트 dto
     */
    @Transactional
    public void editEvent(EventDto eventDto) {
        String eventId = eventDto.getEventId();
        Optional<EventMetadata> metadataOpt = emRepository.findFirstByEventId(eventId);
        EventMetadata eventMetadata = metadataOpt
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
        eventMetadata.updateName(eventDto.getName());
        eventMetadata.updateDescription(eventDto.getDescription());
        eventMetadata.updateStartTime(eventDto.getStartTime());
        eventMetadata.updateEndTime(eventDto.getEndTime());
        eventMetadata.updateUrl(eventDto.getUrl());

        EventFieldMapper mapper = mapperMatcher.getMapper(eventDto.getEventType());
        if(mapper == null) throw new EventException(ErrorCode.INVALID_EVENT_TYPE);

        mapper.editEventField(eventMetadata, eventDto);
        log.info("Edited event: {}", eventId);
        emRepository.save(eventMetadata);
    }

    /**
     * 이벤트에 대한 초기 데이터 정보를 제공한다.
     * @param eventId 요청한 이벤트의 id
     * @return 이벤트 내용을 담은 dto
     */
    @Transactional(readOnly = true)
    public EventDto getEventInfo(String eventId) {
        Optional<EventMetadata> metadataOpt = emRepository.findFirstByEventId(eventId);
        EventMetadata metadata = metadataOpt
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));

        EventFieldMapper mapper = mapperMatcher.getMapper(metadata.getEventType());
        if(mapper == null) throw new EventException(ErrorCode.INVALID_EVENT_TYPE);

        EventDto eventDto = EventDto.builder()
                .eventId(metadata.getEventId())
                .name(metadata.getName())
                .description(metadata.getDescription())
                .url(metadata.getUrl())
                .startTime(metadata.getStartTime())
                .endTime(metadata.getEndTime())
                .eventType(metadata.getEventType())
                .build();

        mapper.fetchToDto(metadata, eventDto);
        log.info("Fetched Event Info: {}", eventId);
        return eventDto;
    }

    /**
     * 매칭되는 이벤트를 탐색한다
     * @param search 이벤트 검색 내용
     * @param sortQuery 정렬 내용이 담긴 쿼리
     * @param page 현재 페이지
     * @param size 페이지의 크기
     * @return 매칭된 이벤트 목록
     */
    @Transactional(readOnly = true)
    public BriefEventPageDto searchEvents(String search, String sortQuery, String typeQuery, Integer page, Integer size) {
        // findBy를 이용하려면 Sort와 Page를 하나로 몰아넣으면 안된다.
        Sort sort = parseSort(sortQuery);
        Set<EventType> types = parseTypes(typeQuery);

        PageRequest pageInfo = PageRequest.of(
                page != null ? page : EventConst.EVENT_DEFAULT_PAGE,
                size != null ? size : EventConst.EVENT_DEFAULT_SIZE
        );

        var searchOnName = EventSpecification.searchOnName(search);
        var searchOnEventId = EventSpecification.searchOnEventId(search);
        var eventTypeIn = EventSpecification.isEventTypeIn(types);

        Page<BriefEventDto> eventPage = emRepository.findBy(
                searchOnName.or(searchOnEventId)
                        .and(eventTypeIn),
                (p) -> p.as(BriefEventDto.class)
                        .sortBy(sort)
                        .page(pageInfo)
        );

        return BriefEventPageDto.from(eventPage);
    }

    private Set<EventType> parseTypes(String typeQuery) {
        Set<EventType> result = new HashSet<>();
        if(typeQuery == null) return result;

        String[] types = typeQuery.split(",");
        for(String type : types) {
            try {
                EventType eventType = EventType.valueOf(type);
                result.add(eventType);
            } catch(Exception e) {
                continue;
            }
        }
        return result;
    }

    private Sort parseSort(String sortQuery) {
        List<Sort.Order> orders = new ArrayList<>();
        for(var entries: EventSearchQueryParser.parse(sortQuery).entrySet()){
            String field = entries.getKey();
            String value = entries.getValue().toLowerCase();

            if(!EventConst.sortableFields.contains(field)) continue;
            switch (value) {
                case "asc": case "":
                    orders.add(Sort.Order.asc(field));
                    break;
                case "desc":
                    orders.add(Sort.Order.desc(field));
                    break;
            }
        }
        // findBy를 이용하려면 Sort와 Page를 하나로 몰아넣으면 안된다.
        return Sort.by(orders);
    }

    /**
     * 이벤트 힌트 정보를 받는다. 관리자는 이벤트 힌트 정보를 기반으로 이벤트에 대한 추가적인 정보를 조회할 수 있다.
     * @param search 이벤트 검색어
     * @return 검색을 위한 힌트 정보 ( id, 이름 )
     */
    public List<EventSearchHintDto> searchHints(String search) {
        var searchOnEventIdDefaultReject = EventSpecification.searchOnEventId(search, false);
        var isDrawEvent = EventSpecification.isEventTypeOf(EventType.draw);
        // 내부적으로는 모든 데이터를 fetch하는 문제가 여전히 존재.

        log.info("searching hints for {}", search);
        return emRepository.findBy(
                searchOnEventIdDefaultReject.and(isDrawEvent),
                (q) -> q.as(EventSearchHintDto.class).all()
        );
    }

    /**
     * 이벤트 프레임을 생성한다.
     * @param name 이벤트 프레임의 이름
     */
    @Transactional
    public void createEventFrame(String frameId, String name) {
        EventFrame eventFrame = EventFrame.of(frameId, name);
        efRepository.save(eventFrame);
        log.info("Created Event Frame: {}", name);
    }

}
