package hyundai.softeer.orange.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RandomStrGeneratorTest {
    @DisplayName("generate는 지정된 길이의 랜덤 문자열을 생성한다.")
    @Test
    void testRandomStr() {
        int length = 20;

        String randStr = RandomStrGenerator.generate(length);
        assertThat(randStr).isNotBlank().hasSize(length);
        assertThat(randStr).matches("[a-zA-Z0-9]{" + length + "}");
    }
}