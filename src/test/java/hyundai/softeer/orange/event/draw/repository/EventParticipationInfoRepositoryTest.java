package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.support.TCDataJpaTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value = "classpath:sql/EventParticipationInfoRepositoryTest.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TCDataJpaTest
class EventParticipationInfoRepositoryTest {
    @Autowired
    EventParticipationInfoRepository epiRepository;

    @DisplayName("존재하는 draw 이벤트는 참여자 정보 반환")
    @Test
    void getParticipationCountPerUserIfDrawEventExist() {
        var participationCounts = epiRepository.countPerEventUserByEventId(1L);
        participationCounts.sort((a,b) -> (int) (a.getEventUserId() - b.getEventUserId()));
        assertThat(participationCounts).hasSize(3);
        assertThat(participationCounts.get(0).getCount()).isEqualTo(3);
        assertThat(participationCounts.get(1).getCount()).isEqualTo(6);
        assertThat(participationCounts.get(2).getCount()).isEqualTo(2);
    }

    @DisplayName("존재하지 않는 draw 이벤트는 빈 배열 반환")
    @Test
    void getEmptyArrIfDrawEventNotExist() {
        var participationCounts = epiRepository.countPerEventUserByEventId(10L);
        assertThat(participationCounts).isEmpty();
    }

    @DisplayName("draw 이벤트 존재해도 유저 없으면 안함")
    @Test
    void getEmptyArrIfDrawEventExistButUserNotExist() {
        var participationCounts = epiRepository.countPerEventUserByEventId(2L);
        assertThat(participationCounts).isEmpty();
    }
}