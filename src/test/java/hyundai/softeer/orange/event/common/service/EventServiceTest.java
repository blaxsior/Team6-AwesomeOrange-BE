package hyundai.softeer.orange.event.common.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.core.JsonUtil;
import hyundai.softeer.orange.event.common.EventConst;
import hyundai.softeer.orange.event.common.component.eventFieldMapper.EventFieldMapperMatcher;
import hyundai.softeer.orange.event.common.component.eventFieldMapper.mapper.FcfsEventFieldMapper;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.component.EventKeyGenerator;
import hyundai.softeer.orange.event.dto.EventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EventServiceTest {

    @InjectMocks
    private EventService eventService;

    @Mock
    private EventFrameRepository efRepo;

    @Mock
    private EventMetadataRepository emRepo;

    @Mock
    private EventFieldMapperMatcher mapperMatcher;

    @Mock
    private EventKeyGenerator keyGenerator;

    @Mock
    private RedisTemplate<String, Object> eventDtoRedisTemplate;

    @Mock
    private JsonUtil jsonUtil;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("대응되는 eventframe이 없으면 생성")
    @Test
    void createEvent_throwErrorIFNoMatchedEventFrame() {
        EventDto eventDto = EventDto.builder().build();

        assertThatThrownBy(() -> {
            eventService.createEvent(eventDto);
        }).isInstanceOf(EventException.class);

        verify(efRepo, times(1)).save(any(EventFrame.class));
    }

    @DisplayName("이벤트 타입에 매칭되는 매퍼가 없다면 예외 반환")
    @Test
    void createEvent_throwErrorIFNoMatchedEventMapper() {
        EventDto eventDto = mock(EventDto.class);
        when(eventDto.getEventFrameId()).thenReturn("testtag");
        when(eventDto.getEventType()).thenReturn(EventType.fcfs);
        when(efRepo.findByFrameId(anyString())).thenReturn(Optional.of(EventFrame.of("the-new-ioniq5","test")));

        assertThatThrownBy(() -> {
            eventService.createEvent(eventDto);
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.INVALID_EVENT_TYPE.getErrorCode());
    }

    @DisplayName("이벤트 타입에 매칭되는 매퍼도 있다면 정상적으로 실행됨.")
    @Test
    void createEvent_presistEventMetadataSuccessfully() {
        EventDto eventDto = mock(EventDto.class);
        when(eventDto.getEventFrameId()).thenReturn("testtag");
        when(eventDto.getEventType()).thenReturn(EventType.fcfs);
        when(efRepo.findByFrameId(anyString())).thenReturn(Optional.of(EventFrame.of("the-new-ioniq5","test")));
        when(mapperMatcher.getMapper(any(EventType.class))).thenReturn(mock(FcfsEventFieldMapper.class));

        eventService.createEvent(eventDto);

        verify(emRepo, times(1)).save(any(EventMetadata.class));
    }

    @DisplayName("대응되는 임시 이벤트가 있다면 불러옴")
    @Test
    void getTempEvent_GetEventIfExist() {
        long adminId = 10;
        String expectedKey = EventConst.ADMIN_TEMP(adminId);

        EventDto eventDto = mock(EventDto.class);
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(eventDtoRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenReturn(Optional.of(eventDto));

        eventService.getTempEvent(adminId);

        verify(eventDtoRedisTemplate, times(1)).opsForValue();
        verify(ops, times(1)).get(expectedKey);
        verify(jsonUtil, times(1)).parseObj(any(), any());
    }

    @DisplayName("대응되는 임시 이벤트가 없다면 예외")
    @Test
    void getTempEvent_throwIfEventNotFound() {
        long adminId = 10;
        String expectedKey = EventConst.ADMIN_TEMP(adminId);

        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(eventDtoRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.get(expectedKey)).thenReturn(null);

        assertThatThrownBy(() -> {
            eventService.getTempEvent(adminId);
        }).isInstanceOf(EventException.class)
        .hasMessage(ErrorCode.TEMP_EVENT_NOT_FOUND.getErrorCode());
    }

    @DisplayName("이벤트를 임시 저장")
    @Test
    void saveTempEvent_successfully() {
        long adminId = 10;
        String expectedKey = EventConst.ADMIN_TEMP(adminId);
        ValueOperations<String, Object> ops = mock(ValueOperations.class);
        when(eventDtoRedisTemplate.opsForValue()).thenReturn(ops);
        EventDto eventDto = mock(EventDto.class);
        eventService.saveTempEvent(adminId, eventDto);
        verify(eventDtoRedisTemplate, times(1)).opsForValue();
        verify(ops, times(1)).set(eq(expectedKey), eq(eventDto), anyLong(), eq(TimeUnit.HOURS));
    }

    @DisplayName("이벤트가 없다면 404")
    @Test
    void deleteEvent_throwIfEventNotFound() {
        String eventId = "test";
        when(emRepo.findFirstByEventId(anyString())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> {
            eventService.deleteEvent(eventId);
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getErrorCode());
    }

    @DisplayName("이벤트가 진행 중이면 삭제 불가")
    @Test
    void deleteEvent_throwIfEventRunning() {
        String eventId = "test";
        Instant now = Instant.now();

        Instant start = now.minus(1, ChronoUnit.HOURS);
        Instant end = now.plus(1, ChronoUnit.HOURS);
        EventMetadata eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .startTime(start)
                .endTime(end)
                .build();
        when(emRepo.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        assertThatThrownBy(() -> {
            eventService.deleteEvent(eventId);
        }).isInstanceOf(EventException.class)
        .hasMessage(ErrorCode.CANNOT_DELETE_EVENT_RUNNING.getErrorCode());
    }

    @DisplayName("이벤트가 완료되었으면 삭제 불가")
    @Test
    void deleteEvent_throwIfEventEnded() {
        String eventId = "test";
        Instant now = Instant.now();

        Instant start = now.minus(2, ChronoUnit.HOURS);
        Instant end = now.minus(1, ChronoUnit.HOURS);
        EventMetadata eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .startTime(start)
                .endTime(end)
                .build();
        when(emRepo.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        assertThatThrownBy(() -> {
            eventService.deleteEvent(eventId);
        }).isInstanceOf(EventException.class)
        .hasMessage(ErrorCode.CANNOT_DELETE_EVENT_ENDED.getErrorCode());
    }

    @DisplayName("이벤트가 시작되기 전이라면 삭제 가능")
    @Test
    void deleteEvent_successfullyBeforeStart() {
        String eventId = "test";
        Instant now = Instant.now();

        Instant start = now.plus(1, ChronoUnit.HOURS);
        Instant end = now.plus(2, ChronoUnit.HOURS);
        EventMetadata eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .startTime(start)
                .endTime(end)
                .build();
        when(emRepo.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        eventService.deleteEvent(eventId);
        verify(emRepo, times(1)).delete(any(EventMetadata.class));
    }
}