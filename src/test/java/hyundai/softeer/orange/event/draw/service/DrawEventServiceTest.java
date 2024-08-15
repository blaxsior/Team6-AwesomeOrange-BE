package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.event.common.EventConst;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.draw.component.picker.PickTarget;
import hyundai.softeer.orange.event.draw.component.picker.WinnerPicker;
import hyundai.softeer.orange.event.draw.component.score.ScoreCalculator;
import hyundai.softeer.orange.event.draw.dto.DrawEventWinningInfoBulkInsertDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.entity.DrawEventMetadata;
import hyundai.softeer.orange.event.draw.exception.DrawEventException;
import hyundai.softeer.orange.event.draw.repository.DrawEventRepository;
import hyundai.softeer.orange.event.draw.repository.DrawEventWinningInfoRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class DrawEventServiceTest {
    EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
    DrawEventDrawMachine machine = mock(DrawEventDrawMachine.class);
    StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);

    @AfterEach
    void afterEach() {
        reset(emRepository,machine,redisTemplate);
    }

    @DisplayName("대응되는 이벤트가 존재하지 않으면 예외 반환")
    @Test
    void draw_throwIfDrawEventNotFound() {
        String eventId = "test-key";
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.empty());

        var deService = new DrawEventService(emRepository, machine, redisTemplate);

        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
    }

    @DisplayName("추첨 이벤트가 아니면 예외 반환")
    @Test
    void draw_throwIfNotDrawType() {
        String eventId = "test-key";
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.fcfs)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        var deService = new DrawEventService(emRepository, machine, redisTemplate);
        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
    }


    @DisplayName("이벤트가 종료되지 않았다면 예외 반환")
    @Test
    void draw_throwIfEventNotEnded() {
        String eventId = "test-key";
        var endTime = LocalDateTime.now().plusDays(10);
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.draw)
                .endTime(endTime)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        var deService = new DrawEventService(emRepository, machine, redisTemplate);
        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
    }


    @DisplayName("추첨 이벤트를 가져올 수 없다면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsNull() {
        String eventId = "test-key";
        var endTime = LocalDateTime.now().plusDays(-10);
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.draw)
                .endTime(endTime)
                .drawEvent(null)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        var deService = new DrawEventService(emRepository, machine, redisTemplate);
        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
    }

    @DisplayName("추첨 이벤트가 이미 추첨된 상태라면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsAlreadyDrawn() {
        String eventId = "test-key";
        var endTime = LocalDateTime.now().plusDays(-10);
        var drawEvent = new DrawEvent();
        drawEvent.setDrawn(true);
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.draw)
                .endTime(endTime)
                .drawEvent(drawEvent)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));

        var deService = new DrawEventService(emRepository, machine, redisTemplate);
        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
    }

    @DisplayName("이벤트가 현재 추첨 중이라면 예외 반환")
    @Test
    void draw_throwIfDrawEventIsDrawing() {
        String eventId = "test-key";
        String key = EventConst.IS_DRAWING(eventId);
        var endTime = LocalDateTime.now().plusDays(-10);
        var drawEvent = new DrawEvent();
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.draw)
                .endTime(endTime)
                .drawEvent(drawEvent)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        // 현재 진입 중인 사람이 있음
        when(ops.increment(key)).thenReturn(2L);
        when(redisTemplate.opsForValue()).thenReturn(ops);

        var deService = new DrawEventService(emRepository, machine, redisTemplate);

        assertThatThrownBy(() -> {
            deService.draw(eventId);
        });
        verify(ops, times(1)).increment(key);
        verify(redisTemplate, times(1)).opsForValue();
    }

    @DisplayName("이벤트 추첨 조건이 된다면 추첨 진행")
    @Test
    void draw_successfullyDraw() throws InterruptedException {
        String eventId = "test-key";
        String key = EventConst.IS_DRAWING(eventId);
        var endTime = LocalDateTime.now().plusDays(-10);
        var drawEvent = new DrawEvent();
        var eventMetadata = EventMetadata.builder()
                .eventId(eventId)
                .eventType(EventType.draw)
                .endTime(endTime)
                .drawEvent(drawEvent)
                .build();
        when(emRepository.findFirstByEventId(eventId)).thenReturn(Optional.of(eventMetadata));
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(ops.increment(key)).thenReturn(1L);
        when(redisTemplate.opsForValue()).thenReturn(ops);
        CompletableFuture<Void> future = CompletableFuture.completedFuture(null);
        when(machine.draw(any(DrawEvent.class))).thenReturn(future);

        var deService = new DrawEventService(emRepository, machine, redisTemplate);
        deService.draw("test-key");
        future.join(); // 비동기 끝날 때까지 대기 -> delete 실행되는지 검사
        TimeUnit.SECONDS.sleep(1L);

        verify(ops, times(1)).increment(key);
        verify(redisTemplate, times(1)).delete(key);
        verify(redisTemplate, times(1)).opsForValue();
        verify(machine, times(1)).draw(any(DrawEvent.class));
    }
}