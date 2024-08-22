package hyundai.softeer.orange.event.common.service;

import hyundai.softeer.orange.event.common.component.eventFieldMapper.EventFieldMapperMatcher;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.component.EventKeyGenerator;
import hyundai.softeer.orange.event.dto.BriefEventDto;
import hyundai.softeer.orange.support.IntegrationDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EventServiceSearchEventsTest extends IntegrationDataJpaTest {
    @Autowired
    private EventMetadataRepository emRepo;
    @Autowired
    private EventFrameRepository efRepo;

    private EventFieldMapperMatcher mapperMatcher;
    private EventKeyGenerator keyGenerator;
    private EventService eventService;

    @BeforeEach
    public void setUp() {
        mapperMatcher = mock(EventFieldMapperMatcher.class);
        keyGenerator = mock(EventKeyGenerator.class);
        eventService = new EventService(efRepo, emRepo, mapperMatcher, keyGenerator, null, null);
        EventFrame ef = EventFrame.of("the-new-ioniq5","test");
        efRepo.save(ef);
        EventMetadata em1 = EventMetadata.builder()
                .eventId("HD240805_001")
                .name("hyundai car event")
                .startTime(LocalDateTime.of(2024,8,1,15,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,2,15,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        EventMetadata em2 = EventMetadata.builder()
                .eventId("HD240805_002")
                .name("hello bye")
                .startTime(LocalDateTime.of(2024,8,1,15,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,2,17,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        EventMetadata em3 = EventMetadata.builder()
                .eventId("HD240805_003")
                .name("hyundai car event")
                .startTime(LocalDateTime.of(2024,8,1,18,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,1,20,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        EventMetadata em4 = EventMetadata.builder()
                .eventId("HD240805_004")
                .name("25 always opened")
                .startTime(LocalDateTime.of(2024,8,1,19,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,1,20,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        EventMetadata em5 = EventMetadata.builder()
                .eventId("HD240805_005")
                .name("zebra car event")
                .startTime(LocalDateTime.of(2024,8,1,21,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,1,22,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        EventMetadata em6 = EventMetadata.builder()
                .eventId("HD240805_006")
                .name("25 always opened")
                .startTime(LocalDateTime.of(2024,8,1,22,0).atZone(ZoneOffset.UTC).toInstant())
                .endTime(LocalDateTime.of(2024,8,1,23,0).atZone(ZoneOffset.UTC).toInstant())
                .eventFrame(ef)
                .build();

        emRepo.saveAll(List.of(em1,em2,em3,em4,em5,em6));
    }

    @DisplayName("search 없으면 모두 출력")
    @Test
    void searchEvents_findAllIfSearchIsNull() {
        var page = eventService.searchEvents(null, null, null,null, null);
        var list = page.getContents();
        assertThat(list).hasSize(5);
    }

    @DisplayName("search 있으면 매칭되는 값 출력")
    @Test
    void searchEvents_findMatchedIfSearchExists() {
        var page = eventService.searchEvents("hyundai", null, null,null, null);
        var list = page.getContents();
        assertThat(list).hasSize(2);
    }

    @DisplayName("search 있더라도 매칭되는 것 없으면 아무것도 반환 안함")
    @Test
    void searchEvents_findNothingIfSearchExistsButNotMatch() {
        var page = eventService.searchEvents("not-exist", null, null,null, null);
        var list = page.getContents();
        assertThat(list).hasSize(0);
    }

    @DisplayName("정렬 옵션 있으면 정렬된 형태로 반환")
    @Test
    void searchEvents_returnOrdered() {
        String query = "startTime,endTime:desc,error";
        // startTime은 존재, 기본 값 asc
        // endTime은 존재, desc
        // error은 존재 X, 맞지 않는 값은 그냥 무시

        var list = eventService.searchEvents(null, "startTime,endTime:desc,error", null,null, null);
        BriefEventDto target = list.getContents().get(0);

        assertThat(target.getEventId()).isEqualTo("HD240805_002");
    }

    @DisplayName("페이지 옵션 있다면 해당 데이터 반환")
    @Test
    void searchEvents_returnPaged() {
        String query = "startTime,endTime:desc,error";
        // startTime은 존재, 기본 값 asc
        // endTime은 존재, desc
        // error은 존재 X, 맞지 않는 값은 그냥 무시

        var page = eventService.searchEvents(null, "eventId", null,1, 2);
        var list = page.getContents();

        assertThat(list.get(0).getEventId()).isEqualTo("HD240805_003");
        assertThat(list.get(1).getEventId()).isEqualTo("HD240805_004");
    }

    @DisplayName("여러 옵션 함께 사용도 가능")
    @Test
    void searchEvents_withMultipleOptions() {
        var page = eventService.searchEvents("25", "endTime:desc", null,1, 1);
        BriefEventDto target = page.getContents().get(0);

        assertThat(page.getContents()).hasSize(1);
        assertThat(target.getEventId()).isEqualTo("HD240805_004");
    }
}
