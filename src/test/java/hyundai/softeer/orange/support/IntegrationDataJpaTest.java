package hyundai.softeer.orange.support;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@TCDataJpaTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationDataJpaTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 이 클래스를 상속받는 모든 테스트 클래스는 모든 테스트가 끝나면 모든 테이블의 데이터를 삭제한다.
    @AfterAll
    void tearDownAll() {
        clear();
    }

    private void clear(){
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tableNameList = jdbcTemplate.queryForList("SHOW TABLES", String.class); // 모든 테이블 이름을 가져온다.
        for(String tableName : tableNameList) {
            jdbcTemplate.execute("TRUNCATE TABLE " + tableName); // 모든 테이블의 데이터를 삭제한다.
            jdbcTemplate.execute("ALTER TABLE " + tableName + " AUTO_INCREMENT = 1"); // AUTO_INCREMENT 초기화
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }
}
