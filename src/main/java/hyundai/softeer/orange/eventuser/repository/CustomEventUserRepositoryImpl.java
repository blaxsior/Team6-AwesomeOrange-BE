package hyundai.softeer.orange.eventuser.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import hyundai.softeer.orange.event.common.entity.QEventFrame;
import hyundai.softeer.orange.eventuser.dto.EventUserScoreDto;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.entity.QEventUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@RequiredArgsConstructor
@Repository
public class CustomEventUserRepositoryImpl implements CustomEventUserRepository {
    private final JdbcTemplate jdbcTemplate;
    private final JPAQueryFactory queryFactory;

    @Override
    public void updateScoreMany(List<EventUserScoreDto> userScores) {
        String sql = "UPDATE event_user SET score = ? WHERE id = ?";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EventUserScoreDto userScore = userScores.get(i);
                ps.setLong(1, userScore.score());
                ps.setLong(2, userScore.userId());
            }

            @Override
            public int getBatchSize() {
                return userScores.size();
            }
        });
    }

    @Override
    public Page<EventUser> findBySearch(String search, String field, Pageable pageable) {
        QEventUser user = QEventUser.eventUser;
        QEventFrame eventFrame = QEventFrame.eventFrame;

        var query =  queryFactory.select(user)
                .from(user)
                .leftJoin(user.eventFrame, eventFrame)
                .fetchJoin();

        if("userName".equals(field)) query.where(user.userName.contains(search));
        else if("phoneNumber".equals(field))query.where(user.phoneNumber.contains(search));
        else if("frameId".equals(field)) query.where(user.eventFrame.frameId.contains(search));

        var data = query.offset(pageable.getOffset()).limit(pageable.getPageSize()).fetch();
        return new PageImpl<>(data, pageable, data.size());
    }
}
