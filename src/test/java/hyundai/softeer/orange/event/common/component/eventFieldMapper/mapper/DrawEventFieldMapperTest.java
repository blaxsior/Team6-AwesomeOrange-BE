package hyundai.softeer.orange.event.common.component.eventFieldMapper.mapper;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.draw.enums.DrawEventAction;
import hyundai.softeer.orange.event.draw.exception.DrawEventException;
import hyundai.softeer.orange.event.draw.repository.DrawEventMetadataRepository;
import hyundai.softeer.orange.event.draw.repository.DrawEventScorePolicyRepository;
import hyundai.softeer.orange.event.dto.EventDto;
import hyundai.softeer.orange.event.dto.draw.DrawEventDto;
import hyundai.softeer.orange.event.dto.draw.DrawEventMetadataDto;
import hyundai.softeer.orange.event.dto.draw.DrawEventScorePolicyDto;
import hyundai.softeer.orange.support.IntegrationDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DrawEventFieldMapperTest extends IntegrationDataJpaTest {
    @Autowired
    DrawEventMetadataRepository drawEventMetadataRepository;
    @Autowired
    DrawEventScorePolicyRepository drawEventScorePolicyRepository;
    DrawEventFieldMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new DrawEventFieldMapper(drawEventMetadataRepository, drawEventScorePolicyRepository);
    }


    @DisplayName("canHandle은 해당 타입을 지원하는지 여부를 반환한다.")
    @Test
    void canHandle_returnTypeSupported() {
        EventType supported = EventType.draw;
        EventType unsupported = EventType.fcfs;

        assertThat(mapper.canHandle(supported)).isTrue();
        assertThat(mapper.canHandle(unsupported)).isFalse();
    }

    @DisplayName("EventDto에 draw 필드가 없으면 예외가 터진다.")
    @Test
    void fetchToEventEntity_throwErrorIfFcfsDtoNotExists() {
        EventMetadata metadata = new EventMetadata();
        EventDto dto1 = new EventDto(); // drawDto 없음

        assertThatThrownBy(() -> {
            mapper.fetchToEventEntity(metadata, dto1);
        });
    }

    @DisplayName("정상적인 EventDto가 들어오면 정상적으로 관계를 설정한다.")
    @Test
    void fetchToEventEntity_setRelationIfEventDtoIsValid() {
        EventMetadata metadata = new EventMetadata();
        EventDto dto = mock(EventDto.class);
        DrawEventDto drawEventDto = mock(DrawEventDto.class);
        when(dto.getDraw()).thenReturn(drawEventDto);
        when(drawEventDto.getMetadata()).thenReturn(List.of(new DrawEventMetadataDto()));
        when(drawEventDto.getPolicies()).thenReturn(List.of(new DrawEventScorePolicyDto()));

        mapper.fetchToEventEntity(metadata, dto);

        var drawEvent = metadata.getDrawEvent();
        var drawEventMetadata = drawEvent.getMetadataList();
        var oneDrawMetadata = drawEventMetadata.get(0);
        var drawEventPolicies  = drawEvent.getPolicyList();
        var oneEventPolicy = drawEventPolicies.get(0);

        assertThat(drawEventMetadata).isNotNull().hasSize(1);
        assertThat(drawEventPolicies).isNotNull().hasSize(1);
        // 부모 자식 관계 잘 설정하는지
        assertThat(oneDrawMetadata.getDrawEvent()).isSameAs(drawEvent);
        assertThat(oneEventPolicy.getDrawEvent()).isSameAs(drawEvent);
    }

    @DisplayName("draw 이벤트가 없으면 예외가 발생한다.")
    @Test
    void editEventField_throwIfDrawEventNotExists() {
        EventMetadata metadata = new EventMetadata();
        EventDto dto = mock(EventDto.class);
        assertThatThrownBy(() -> {
            mapper.editEventField(metadata, dto);
        });
    }

    @DisplayName("validateDrawEventDto: 등수에 중복이 있다면 예외 반환")
    @Test
    void validateDrawEventDto_throwIfDuplicatedGrade() {
        DrawEventDto dto = new DrawEventDto();
        dto.setMetadata(
                List.of(
                        DrawEventMetadataDto.builder().grade(1L).build(),
                        DrawEventMetadataDto.builder().grade(1L).build()
                )
        );

        dto.setPolicies(List.of(
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.ParticipateEvent)
                        .score(1)
                        .build(),
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.WriteComment)
                        .build()
        ));

        assertThatThrownBy(() -> {
            mapper.validateDrawEventDto(dto);
        }).isInstanceOf(DrawEventException.class)
        .hasMessage(ErrorCode.DUPLICATED_GRADES.getMessage());
    }

    @DisplayName("validateDrawEventDto: 액션에 중복이 있다면 예외 반환")
    @Test
    void validateDrawEventDto_throwIfDuplicatedAction() {
        DrawEventDto dto = new DrawEventDto();
        dto.setMetadata(List.of(DrawEventMetadataDto.builder().grade(1L).build()));
        dto.setPolicies(List.of(
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.ParticipateEvent).score(1)
                        .build(),
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.ParticipateEvent).score(5)
                        .build()
        ));

        assertThatThrownBy(() -> {
            mapper.validateDrawEventDto(dto);
        }).isInstanceOf(DrawEventException.class)
                .hasMessage(ErrorCode.DUPLICATED_POLICIES.getMessage());
    }

    @DisplayName("validateDrawEventDto: validation 성공")
    @Test
    void validateDrawEventDto_validateSuccessfully() {
        DrawEventDto dto = new DrawEventDto();
        dto.setMetadata(List.of(
                        DrawEventMetadataDto.builder().grade(1L).build(),
                        DrawEventMetadataDto.builder().grade(2L).build()
        ));

        dto.setPolicies(List.of(
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.ParticipateEvent)
                        .score(1)
                        .build(),
                DrawEventScorePolicyDto.builder()
                        .action(DrawEventAction.ParticipateEvent)
                        .build()
        ));
        assertThatThrownBy(() -> {
            mapper.validateDrawEventDto(null);
        }).isInstanceOf(EventException.class)
                .hasMessage(ErrorCode.INVALID_JSON.getMessage());
    }
}