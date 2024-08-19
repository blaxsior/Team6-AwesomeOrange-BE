package hyundai.softeer.orange.event.common.component.eventFieldMapper.mapper;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.dto.EventDto;
import hyundai.softeer.orange.event.dto.fcfs.FcfsEventDto;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEvent;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FcfsEventFieldMapperTest {

    FcfsEventFieldMapper mapper;
    @BeforeEach
    void setUp() {
        FcfsEventRepository repo = mock(FcfsEventRepository.class);
        mapper = new FcfsEventFieldMapper(repo);
    }

    @DisplayName("canHandle은 해당 타입을 지원하는지 여부를 반환한다.")
    @Test
    void canHandle_returnTypeSupported() {
        EventType supported = EventType.fcfs;
        EventType unsupported = EventType.draw;

        assertThat(mapper.canHandle(supported)).isTrue();
        assertThat(mapper.canHandle(unsupported)).isFalse();
    }

    @DisplayName("EventDto에 Fcfs 필드가 없으면 예외가 터진다.")
    @Test
    void throwErrorIfFcfsDtoNotExists() {
        EventMetadata metadata = new EventMetadata();
        EventDto dto1 = new EventDto(); // fcfsdto 없음
        EventDto dto2 = mock(EventDto.class);
        when(dto2.getFcfs()).thenReturn(List.of());

        assertThatThrownBy(() -> {
            mapper.fetchToEventEntity(metadata, dto1);
        });

        assertThatThrownBy(() -> {
            mapper.fetchToEventEntity(metadata, dto2);
        });
    }

    @DisplayName("EventDto에 값이 있다면 정상적으로 관계를 설정한다.")
    @Test
    void setRelationIfFcfsDtoExists() {
        EventMetadata metadata = new EventMetadata();
        metadata.updateStartTime(LocalDateTime.of(2024, 8, 1, 0, 0));
        metadata.updateEndTime(LocalDateTime.of(2024, 8, 2, 0, 0));
        EventDto dto = mock(EventDto.class);
        List<FcfsEventDto> dtos = new ArrayList<>();

        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,0,0))
                .endTime(LocalDateTime.of(2024,8,1,4,0))
                .build() // 시작 시간 겹침
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,4,0))
                .endTime(LocalDateTime.of(2024,8,1,8,0))
                .build() // 시간끼리 겹침
        );

        when(dto.getFcfs()).thenReturn(dtos);

        mapper.fetchToEventEntity(metadata, dto);

        List<FcfsEvent> fcfsEvents = metadata.getFcfsEventList();

        assertThat(fcfsEvents).isNotNull().hasSize(2);
        assertThat(fcfsEvents.get(0).getEventMetaData()).isSameAs(metadata);
    }

    @DisplayName("하나라도 기간 외 이벤트 시간대가 있다면 예외 반환")
    @Test
    void validationEventTimes_throwIfThereIsTimeOverBoundary() {
        LocalDateTime startTime = LocalDateTime.of(2024, 8, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 8, 2, 0, 0);
        List<FcfsEventDto> dtos = new ArrayList<>();
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,0,0))
                .endTime(LocalDateTime.of(2024,8,1,4,0))
                .build()
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,4,0))
                .endTime(LocalDateTime.of(2024,8,1,8,0))
                .build()
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,23,0))
                .endTime(LocalDateTime.of(2024,8,2,0,1))
                .build()
        );

        assertThatThrownBy(() -> {
            mapper.validateEventTimes(dtos, startTime, endTime);
        }).isInstanceOf(EventException.class)
        .hasMessage(ErrorCode.INVALID_INPUT_EVENT_TIME.getMessage());
    }

    @DisplayName("겹치는 시간대가 있다면 예외 반환")
    @Test
    void validationEventTimes_throwIfFcfsHasOverlay() {
        LocalDateTime startTime = LocalDateTime.of(2024, 8, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 8, 2, 0, 0);
        List<FcfsEventDto> dtos = new ArrayList<>();
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,0,0))
                .endTime(LocalDateTime.of(2024,8,1,4,0))
                .build()
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,3,0))
                .endTime(LocalDateTime.of(2024,8,1,8,0))
                .build()
        );

        assertThatThrownBy(() -> {
            mapper.validateEventTimes(dtos, startTime, endTime);
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.INVALID_INPUT_EVENT_TIME.getMessage());
    }

    @DisplayName("겹치는 시간대가 없다면 정상적으로 실행")
    @Test
    void validationEventTimes_validateSuccessfully() {
        LocalDateTime startTime = LocalDateTime.of(2024, 8, 1, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2024, 8, 2, 0, 0);
        List<FcfsEventDto> dtos = new ArrayList<>();
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,8,0))
                .endTime(LocalDateTime.of(2024,8,2,0,0))
                .build() // 끝 시간 겹침
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,0,0))
                .endTime(LocalDateTime.of(2024,8,1,4,0))
                .build() // 시작 시간 겹침
        );
        dtos.add(FcfsEventDto.builder()
                .startTime(LocalDateTime.of(2024,8,1,4,0))
                .endTime(LocalDateTime.of(2024,8,1,8,0))
                .build() // 시간끼리 겹침
        );
        // 점에서 겹치는 것은 겹친다고 취급 안한다.

        mapper.validateEventTimes(dtos, startTime, endTime);
    }
}