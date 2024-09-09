package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDateDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.repository.EventParticipationInfoRepository;

import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.exception.EventUserException;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EventParticipationServiceTest {
    EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
    EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);
    EventUserRepository euRepository = mock(EventUserRepository.class);
    Long frameId = 1L;

    @DisplayName("이벤트가 존재하지 않으면 예외 반환")
    @Test
    void getParticipationDateList_throwIfEventNotExist() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.empty());
        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.getParticipationDateList("test", "any");
        });
    }

    @DisplayName("이벤트가 존재해도, draw 이벤트가 아니면 예외 반환")
    @Test
    void getParticipationDateList_EventIsNotDraw() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.fcfs).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.getParticipationDateList("test", "any");
        });
    }

    @DisplayName("이벤트가 draw 이벤트여도 drawEvent == null이면 예외 반환")
    @Test
    void getParticipationDateList_EventIsDrawButDrawEventIsNull() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.draw).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.getParticipationDateList("test", "any");
        });
    }

    @DisplayName("정상적인 이벤트라면 참여 기록 반환")
    @Test
    void getParticipationDateList_returnParticipationInfos() {
        var eventMetadata = EventMetadata.builder()
                .eventType(EventType.draw)
                .build();
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(eventMetadata));
        var mockDto1 = mock(EventParticipationDateDto.class);
        var mockDto2 = mock(EventParticipationDateDto.class);
        when(mockDto1.getDate()).thenReturn(Instant.now());
        when(mockDto2.getDate()).thenReturn(Instant.now());

        when(epiRepository.findByEventUserId(any(), any())).thenReturn(List.of(mockDto1, mockDto2));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, null);
        var list = service.getParticipationDateList("test", "test");
        assertThat(list).isNotNull();
        assertThat(list.dates()).hasSize(2);
    }

    @DisplayName("이벤트가 존재하지 않으면 예외 반환")
    @Test
    void participateAtDaily_throwIfEventNotExist() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.empty());

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        }).hasMessage(ErrorCode.EVENT_NOT_FOUND.getErrorCode());
    }

    @DisplayName("이벤트가 존재해도, draw 이벤트가 아니면 예외 반환")
    @Test
    void participateAtDaily_EventIsNotDraw() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.fcfs).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        }).hasMessage(ErrorCode.EVENT_NOT_FOUND.getErrorCode());
        verify(emRepository, atLeastOnce()).findFirstByEventId(any());
    }

    @DisplayName("이벤트가 draw 이벤트여도 drawEvent == null이면 예외 반환")
    @Test
    void participateDaily_EventIsDrawButDrawEventIsNull() {
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.draw).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.EVENT_NOT_FOUND.getErrorCode());
    }

    @DisplayName("이벤트 유저가 없다면 예외 반환")
    @Test
    void participateDaily_throwIfNotEventTime() {
        var eventMetadata = EventMetadata.builder()
                .eventType(EventType.draw)
                .eventFrameId(frameId)
                .build();
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(eventMetadata));

        when(euRepository.findByUserId(any())).thenReturn(Optional.empty());

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        }).isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.EVENT_USER_NOT_FOUND.getErrorCode());
        verify(euRepository, atLeastOnce()).findByUserId(any());
    }

    @DisplayName("이벤트 유저가 있을 때 이벤트 기간이 지났으면 예외 반환")
    @Test
    void participateAtDate_throwIfNotEventTime() {
        var eventMetadata = EventMetadata.builder()
                .startTime(LocalDateTime.of(2024, 8, 1, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024, 8, 10, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .eventType(EventType.draw)
                .eventFrameId(frameId)
                .build();
        eventMetadata.updateDrawEvent(new DrawEvent());

        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(eventMetadata));

        Instant now = LocalDateTime.of(2024, 9, 1, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant();

        // 유저 있음
        EventUser user = mock(EventUser.class);
        when(user.getEventFrameId()).thenReturn(frameId);
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(user));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateAtDate("test", "any", now);
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.INVALID_EVENT_TIME.getErrorCode());
        verify(epiRepository, never()).existsByEventUserAndDrawEventAndDateBetween(any(), any(), any(), any());
    }

    @DisplayName("오늘 참여했다면 예외 반환")
    @Test
    void participateAtDate_throwIfAlreadyParticipated() {
        var eventMetadata = EventMetadata.builder()
                .startTime(LocalDateTime.of(2024, 8, 1, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024, 8, 10, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .eventType(EventType.draw)
                .eventFrameId(frameId)
                .build();
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(eventMetadata));

        // 이벤트 기간 내
        Instant now = LocalDateTime.of(2024, 8, 3, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant();
        // 오늘 참여함
        when(epiRepository.existsByEventUserAndDrawEventAndDateBetween(any(), any(), any(), any())).thenReturn(true);

        // 유저 있음
        EventUser user = mock(EventUser.class);
        when(user.getEventFrameId()).thenReturn(frameId);
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(user));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateAtDate("test", "any", now);
        }).isInstanceOf(EventException.class)
        .hasMessage(ErrorCode.ALREADY_PARTICIPATED.getErrorCode());
        verify(epiRepository, atLeastOnce()).existsByEventUserAndDrawEventAndDateBetween(any(), any(), any(), any());
    }

    @DisplayName("오늘 처음 참여했다면 정상적으로 참여")
    @Test
    void participateAtDate_participateSuccessfully() {
        var eventMetadata = EventMetadata.builder()
                .startTime(LocalDateTime.of(2024, 8, 1, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024, 8, 10, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant())
                .eventType(EventType.draw)
                .eventFrameId(frameId)
                .build();
        eventMetadata.updateDrawEvent(new DrawEvent());
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(eventMetadata));

        // 이벤트 기간 내
        Instant now = LocalDateTime.of(2024, 8, 3, 0, 0, 0).atZone(ZoneOffset.UTC).toInstant();
        // 오늘 참여안함
        when(epiRepository.existsByEventUserAndDrawEventAndDateBetween(any(), any(), any(), any())).thenReturn(false);

        // 유저 있음
        EventUser user = mock(EventUser.class);
        when(user.getEventFrameId()).thenReturn(frameId);
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(user));
        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        service.participateAtDate("test", "any", now);
        verify(epiRepository, times(1)).existsByEventUserAndDrawEventAndDateBetween(any(), any(), any(), any());
        verify(epiRepository, times(1)).save(any());
    }
}