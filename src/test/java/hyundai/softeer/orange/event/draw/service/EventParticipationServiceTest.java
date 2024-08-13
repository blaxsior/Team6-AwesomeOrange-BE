package hyundai.softeer.orange.event.draw.service;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class EventParticipationServiceTest {
    @DisplayName("이벤트가 존재하지 않으면 예외 반환")
    @Test
    void getParticipationDateList_throwIfEventNotExist() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.empty());
//        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);
//        EventUserRepository euRepository = mock(EventUserRepository.class);

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.getParticipationDateList("test", "any");
        });
    }

    @DisplayName("이벤트가 존재해도, draw 이벤트가 아니면 예외 반환")
    @Test
    void getParticipationDateList_EventIsNotDraw() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
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
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
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
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder()
                        .eventType(EventType.draw)
                        .drawEvent(new DrawEvent())
                        .build()
        ));
        var mockDto1 = mock(EventParticipationDateDto.class);
        var mockDto2 = mock(EventParticipationDateDto.class);
        when(mockDto1.getDate()).thenReturn(LocalDateTime.now());
        when(mockDto2.getDate()).thenReturn(LocalDateTime.now());


        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);
        when(epiRepository.findByEventUserId(any(), any())).thenReturn(List.of(mockDto1, mockDto2));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, null);
        var list = service.getParticipationDateList("test", "test");
        assertThat(list).isNotNull();
        assertThat(list.dates()).hasSize(2);
    }

    @DisplayName("이벤트가 존재하지 않으면 예외 반환")
    @Test
    void participateAtDaily_throwIfEventNotExist() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.empty());

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        });
    }

    @DisplayName("이벤트가 존재해도, draw 이벤트가 아니면 예외 반환")
    @Test
    void participateAtDaily_EventIsNotDraw() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.fcfs).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        });
        verify(emRepository, atLeastOnce()).findFirstByEventId(any());
    }

    @DisplayName("이벤트가 draw 이벤트여도 drawEvent == null이면 예외 반환")
    @Test
    void participateDaily_EventIsDrawButDrawEventIsNull() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder().eventType(EventType.draw).build()
        ));

        EventParticipationService service = new EventParticipationService(null, emRepository, null);

        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        });
    }

    @DisplayName("이벤트 유저가 없다면 예외 반환")
    @Test
    void participateDaily_throwIfNotEventTime() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder()
                        .eventType(EventType.draw)
                        .drawEvent(new DrawEvent())
                        .build()
        ));
        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);

        EventUserRepository euRepository = mock(EventUserRepository.class);
        when(euRepository.findByUserId(any())).thenReturn(Optional.empty());

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateDaily("test", "any");
        }).isInstanceOf(EventUserException.class);
        verify(euRepository, atLeastOnce()).findByUserId(any());
    }

    @DisplayName("이벤트 유저가 있을 때 이벤트 기간이 지났으면 예외 반환")
    @Test
    void participateAtDate_throwIfNotEventTime() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder()
                        .startTime(LocalDateTime.of(2024,8,1,0,0,0))
                        .endTime(LocalDateTime.of(2024,8,10,0,0,0))
                        .eventType(EventType.draw)
                        .drawEvent(new DrawEvent())
                        .build()
        ));
        LocalDateTime now = LocalDateTime.of(2024,9,1,0,0,0);

        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);

        EventUserRepository euRepository = mock(EventUserRepository.class);
        // 유저 있음
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(mock(EventUser.class)));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateAtDate("test", "any", now);
        }).isInstanceOf(EventException.class);
        verify(epiRepository, never()).existsByEventUserAndDrawEventAndDateBetween(any(),any(),any(),any());
    }

    @DisplayName("오늘 참여했다면 예외 반환")
    @Test
    void participateAtDate_throwIfAlreadyParticipated() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder()
                        .startTime(LocalDateTime.of(2024,8,1,0,0,0))
                        .endTime(LocalDateTime.of(2024,9,2,0,0,0))
                        .eventType(EventType.draw)
                        .drawEvent(new DrawEvent())
                        .build()
        ));
        LocalDateTime now = LocalDateTime.of(2024,9,1,0,0,0);

        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);
        // 오늘 참여함
        when(epiRepository.existsByEventUserAndDrawEventAndDateBetween(any(),any(),any(),any())).thenReturn(true);

        EventUserRepository euRepository = mock(EventUserRepository.class);
        // 유저 있음
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(mock(EventUser.class)));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        assertThatThrownBy(() -> {
            service.participateAtDate("test", "any", now);
        }).isInstanceOf(EventException.class);
        verify(epiRepository, atLeastOnce()).existsByEventUserAndDrawEventAndDateBetween(any(),any(),any(),any());
    }

    @DisplayName("오늘 처음 참여했다면 정상적으로 참여")
    @Test
    void participateAtDate_participateSuccessfully() {
        EventMetadataRepository emRepository = mock(EventMetadataRepository.class);
        when(emRepository.findFirstByEventId(anyString())).thenReturn(Optional.of(
                EventMetadata.builder()
                        .startTime(LocalDateTime.of(2024,8,1,0,0,0))
                        .endTime(LocalDateTime.of(2024,9,2,0,0,0))
                        .eventType(EventType.draw)
                        .drawEvent(new DrawEvent())
                        .build()
        ));
        LocalDateTime now = LocalDateTime.of(2024,9,1,0,0,0);

        EventParticipationInfoRepository epiRepository = mock(EventParticipationInfoRepository.class);
        // 오늘 참여함
        when(epiRepository.existsByEventUserAndDrawEventAndDateBetween(any(),any(),any(),any())).thenReturn(false);

        EventUserRepository euRepository = mock(EventUserRepository.class);
        // 유저 있음
        when(euRepository.findByUserId(any())).thenReturn(Optional.of(mock(EventUser.class)));

        EventParticipationService service = new EventParticipationService(epiRepository, emRepository, euRepository);
        service.participateAtDate("test", "any", now);
        verify(epiRepository, atLeastOnce()).existsByEventUserAndDrawEventAndDateBetween(any(),any(),any(),any());
        verify(epiRepository, atLeastOnce()).save(any());
    }
}