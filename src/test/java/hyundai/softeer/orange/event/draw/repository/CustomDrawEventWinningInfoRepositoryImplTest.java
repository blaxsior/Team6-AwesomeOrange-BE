package hyundai.softeer.orange.event.draw.repository;

import hyundai.softeer.orange.event.draw.dto.DrawEventWinningInfoBulkInsertDto;
import hyundai.softeer.orange.support.TCDataJpaTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(value="classpath:sql/CustomDrawEventWinningInfoRepositoryImplTest.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@TCDataJpaTest
class CustomDrawEventWinningInfoRepositoryImplTest {
    @Autowired // DrawEventWinningInfoRepository로 접근 가능해야 함
    private DrawEventWinningInfoRepository repo;

    @DisplayName("제대로 데이터를 삽입하는지 확인")
    @Test
    void testBulkInsertWork() {
        // Executing prepared SQL statement [insert into draw_event_winning_info (event_user_id, ranking, draw_event_id) VALUES (?, ?, ?)]

        List<DrawEventWinningInfoBulkInsertDto> targets = List.of(
                DrawEventWinningInfoBulkInsertDto.of(1, 1, 1),
                DrawEventWinningInfoBulkInsertDto.of(2, 1, 1),
                DrawEventWinningInfoBulkInsertDto.of(3, 2, 1),
                DrawEventWinningInfoBulkInsertDto.of(4, 2, 1),
                DrawEventWinningInfoBulkInsertDto.of(5, 2, 1),
                DrawEventWinningInfoBulkInsertDto.of(6, 2, 1),
                DrawEventWinningInfoBulkInsertDto.of(1, 1, 2),
                DrawEventWinningInfoBulkInsertDto.of(2, 1, 2),
                DrawEventWinningInfoBulkInsertDto.of(3, 2, 2),
                DrawEventWinningInfoBulkInsertDto.of(4, 2, 2),
                DrawEventWinningInfoBulkInsertDto.of(5, 2, 2),
                DrawEventWinningInfoBulkInsertDto.of(6, 2, 2)
        );
        repo.insertMany(targets);
        var events = repo.findAll();
        assertThat(events).hasSize(12);
    }
}