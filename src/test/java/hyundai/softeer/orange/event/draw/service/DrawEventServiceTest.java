package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.EventConst;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.entity.DrawEventWinningInfo;
import hyundai.softeer.orange.event.draw.enums.DrawEventStatus;
import hyundai.softeer.orange.event.draw.exception.DrawEventException;
import hyundai.softeer.orange.event.draw.repository.DrawEventWinningInfoRepository;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DrawEventServiceTest {

    @InjectMocks
    DrawEventService deService;

    @Mock
    EventMetadataRepository emRepository;

    @Mock
    DrawEventWinningInfoRepository deWinningInfoRepository;

    @Mock
    DrawEventDrawMachine machine;

    @Mock
    StringRedisTemplate redisTemplate;

    String eventId = "test-key";

    private EventMetadata createEventMetadata(String eventId, EventType eventType, LocalDateTime endTime) {
        return EventMetadata.builder()
                .eventId(eventId)
                .eventType(eventType)
                .endTime(endTime)
                .build();
    }

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
    }

    @DisplayName("대응되는 이벤트가 존재하지 않으면 예외 반환")
    @Test
    void draw_throwIfDrawEventNotFound() {
        // given
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
    }

    @DisplayName("추첨 이벤트가 아니면 예외 반환")
    @Test
    void draw_throwIfNotDrawType() {
        // given
        var eventMetadata = createEventMetadata(eventId, EventType.fcfs, LocalDateTime.now());
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
    }


    @DisplayName("이벤트가 종료되지 않았다면 예외 반환")
    @Test
    void draw_throwIfEventNotEnded() {
        // given
        var endTime = LocalDateTime.now().plusDays(10);
        var eventMetadata = createEventMetadata(eventId, EventType.draw, endTime);
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_ENDED.getMessage());
    }


    @DisplayName("추첨 이벤트를 가져올 수 없다면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsNull() {
        // given
        var endTime = LocalDateTime.now().plusDays(-10);
        var eventMetadata = createEventMetadata(eventId, EventType.draw, endTime);
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
    }

    @DisplayName("추첨 이벤트가 이미 추첨된 상태라면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsAlreadyDrawn() {
        // given
        var endTime = LocalDateTime.now().plusDays(-10);
        var drawEvent = new DrawEvent();
        drawEvent.setDrawn(true);
        var eventMetadata = createEventMetadata(eventId, EventType.draw, endTime);
        eventMetadata.updateDrawEvent(drawEvent);
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.ALREADY_DRAWN.getMessage());
    }

    @DisplayName("이벤트가 현재 추첨 중이라면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsDrawing() {
        // given
        String key = EventConst.IS_DRAWING(eventId);
        var endTime = LocalDateTime.now().plusDays(-10);
        var eventMetadata = createEventMetadata(eventId, EventType.draw, endTime);
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        // 현재 진입 중인 사람이 있음
        when(ops.increment(key)).thenReturn(2L);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        // when & then
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_IS_DRAWING.getMessage());
        verify(ops, times(1)).increment(key);
        verify(redisTemplate, times(1)).opsForValue();
    }

    @DisplayName("이벤트 추첨 조건이 된다면 추첨 진행")
    @Test
    void draw_successfullyDraw() throws InterruptedException {
        // given
        String key = EventConst.IS_DRAWING(eventId);
        var endTime = LocalDateTime.now().plusDays(-10);
        var drawEvent = new DrawEvent();
        var eventMetadata = createEventMetadata(eventId, EventType.draw, endTime);
        eventMetadata.updateDrawEvent(drawEvent);
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.increment(key)).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(machine.draw(any(DrawEvent.class))).thenReturn(future);

        // when
        deService.draw(eventId);
        future.join(); // 비동기 끝날 때까지 대기 -> delete 실행되는지 검사
        TimeUnit.SECONDS.sleep(1L);

        // then
        verify(ops, times(1)).increment(key);
        verify(redisTemplate, times(1)).delete(key);
        verify(redisTemplate, times(1)).opsForValue();
        verify(machine, times(1)).draw(any(DrawEvent.class));
    }

    @DisplayName("당첨자 목록 조회")
    @Test
    void getDrawEventWinner() {
        // given
        var eventUser = mock(EventUser.class);
        when(eventUser.getUserName()).thenReturn("test-user");
        when(eventUser.getPhoneNumber()).thenReturn("010-1234-5678");
        var drawEvent = mock(DrawEvent.class);
        when(drawEvent.getId()).thenReturn(1L);
        var eventMetadata = createEventMetadata(eventId, EventType.draw, LocalDateTime.now());
        eventMetadata.updateDrawEvent(drawEvent);
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        when(deWinningInfoRepository.findAllById(drawEvent.getId())).thenReturn(List.of(DrawEventWinningInfo.of(1L, drawEvent, eventUser)));

        // when
        var result = deService.getDrawEventWinner(eventId);

        // then
        assertThat(result).isNotEmpty();
        verify(emRepository, times(1)).findFirstByEventId(eventId);
        verify(deWinningInfoRepository, times(1)).findAllById(drawEvent.getId());
    }

    @DisplayName("getDrawEventStatus: 대응되는 이벤트가 존재하지 않으면 예외 반환")
    @Test
    void getDrawEventStatus_throwIfDrawEventNotFound() {
        var fcfsEventMetadata = createEventMetadata(eventId, EventType.fcfs, LocalDateTime.now());
        var withoutDrawEvent = createEventMetadata(eventId, EventType.draw, LocalDateTime.now());

        when(emRepository.findFirstByEventId(eventId))
                .thenReturn(Optional.empty()) // 이벤트 없는 경우
                .thenReturn(Optional.of(fcfsEventMetadata)) // 이벤트가 draw 아닌 경우
                .thenReturn(Optional.of(withoutDrawEvent));

        // when & then
        // 이벤트 없음
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
        // 이벤트 draw 아님
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
        // 대응되는 draw 객체가 없음
        assertThatThrownBy(() -> deService.draw(eventId))
                .isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getMessage());
    }

    @DisplayName("getDrawEventStatus: 이벤트가 종료되지 않았다면 BEFORE_END")
    @Test
    void getDrawEventStatus_EventNotEnded() {
        LocalDateTime notEndedTime = LocalDateTime.now().plusDays(+10);
        EventMetadata metadata = createEventMetadata(eventId, EventType.draw, notEndedTime);
        metadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(metadata));

        var dto = deService.getDrawEventStatus(eventId);
        assertThat(dto.getStatus()).isEqualTo(DrawEventStatus.BEFORE_END);
    }

    @DisplayName("getDrawEventStatus: 이벤트가 종료되었다면 COMPLETE")
    @Test
    void getDrawEventStatus_EventIsDrawn() {
        var drawEvent = new DrawEvent();
        drawEvent.setDrawn(true);

        LocalDateTime endedTime = LocalDateTime.now().plusDays(-10);
        EventMetadata metadata = createEventMetadata(eventId, EventType.draw, endedTime);
        metadata.updateDrawEvent(drawEvent);
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(metadata));

        var dto = deService.getDrawEventStatus(eventId);
        assertThat(dto.getStatus()).isEqualTo(DrawEventStatus.COMPLETE);
    }

    @DisplayName("getDrawEventStatus: redis에 키가 있다면 추첨 중")
    @Test
    void getDrawEventStatus_EventIsDrawing() {
        var drawEvent = new DrawEvent();
        LocalDateTime endedTime = LocalDateTime.now().plusDays(-10);
        EventMetadata metadata = createEventMetadata(eventId, EventType.draw, endedTime);
        metadata.updateDrawEvent(drawEvent);

        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(metadata));

        ValueOperations<String, String> valueOperation = mock(ValueOperations.class);
        when(valueOperation.get(anyString())).thenReturn("2");
        when(redisTemplate.opsForValue()).thenReturn(valueOperation);

        var dto = deService.getDrawEventStatus(eventId);
        assertThat(dto.getStatus()).isEqualTo(DrawEventStatus.IS_DRAWING);
    }

    @DisplayName("getDrawEventStatus: 선행 조건에 매칭되지 않으면 AVAILABLE")
    @Test
    void getDrawEventStatus_EventIsAvailable() {
        var drawEvent = new DrawEvent();
        LocalDateTime endedTime = LocalDateTime.now().plusDays(-10);
        EventMetadata metadata = createEventMetadata(eventId, EventType.draw, endedTime);
        metadata.updateDrawEvent(drawEvent);

        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(metadata));

        ValueOperations<String, String> valueOperation = mock(ValueOperations.class);
        when(valueOperation.get(anyString())).thenReturn(null);
        when(redisTemplate.opsForValue()).thenReturn(valueOperation);

        var dto = deService.getDrawEventStatus(eventId);
        assertThat(dto.getStatus()).isEqualTo(DrawEventStatus.AVAILABLE);
    }
}