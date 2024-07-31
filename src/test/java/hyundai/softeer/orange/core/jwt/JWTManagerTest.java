package hyundai.softeer.orange.core.jwt;

import hyundai.softeer.orange.admin.entity.Admin;
import hyundai.softeer.orange.core.jwt.JWTManager;
import io.jsonwebtoken.JwtException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

class JWTManagerTest {
    JWTManager jwtManager;
    // generated by web
    static String SECRET_KEY = "9MiWGofHwYvq2nR6VDvmgd6kvXit4ERxb6b7imRRFzk=";

    @BeforeEach
    void setUp() {
        jwtManager = new JWTManager();
        jwtManager.setSecretKey(SECRET_KEY);
    }

    @DisplayName("정상적으로 토큰을 발행하는 상황")
    @Test
    void getSuccessfulToken() {
        String subject = "test";
        String claimKey = "testKey";
        Admin admin = Admin.builder().nickname("testValue").build();
        int lifespan = 5;

        String token = jwtManager.generateToken(subject, Map.of(claimKey, admin), lifespan);
        var tokenInfo = jwtManager.parseToken(token, Map.of(claimKey,Admin.class));

        Admin parsedObj = tokenInfo.getPayload().get(claimKey, Admin.class);

        Assertions.assertThat(parsedObj.getNickname()).isEqualTo("testValue");
        Assertions.assertThat(tokenInfo.getPayload().getSubject()).isEqualTo(subject);
    }

    @DisplayName("잘못된 토큰이 들어오면 예외 반환")
    @Test
    void parseToken_throwExceptionIfTokenInvalid() {
        String wrongToken = "invalid";

        Assertions.assertThatThrownBy(() -> {
            jwtManager.parseToken(wrongToken);
        }).isInstanceOf(JwtException.class);
    }

    @DisplayName("만료된 토큰을 파싱하려 하면 예외 반환")
    @Test
    void parseToken_throwExceptionIfTokenExpired() {
        String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyLCJlYXQiOjE1MTYyMzkwMjJ9.uoxXqWRsbDQpILQcuEgZQDaGzdCuSq1s_mrATP942JY";

        Assertions.assertThatThrownBy(() -> {
            jwtManager.parseToken(expiredToken);
        }).isInstanceOf(JwtException.class);
    }

    @DisplayName("typeMap에 토큰 내 객체의 타입 매핑 정보를 제공하지 않고 claim에서 객체를 바로 얻으려 하면 예외 반환")
    @Test
    void claims_throwExceptionIfTryToGetObjThatNotRegisteredToTypeMap() {
        String subject = "test";
        String claimKey = "testKey";
        Admin admin = Admin.builder().nickname("testValue").build();
        int lifespan = 5;

        String token = jwtManager.generateToken(subject, Map.of(claimKey, admin), lifespan);
        System.out.println(token);
        var tokenInfo = jwtManager.parseToken(token);

        Assertions.assertThatThrownBy(() -> {
            Admin parsedObj = tokenInfo.getPayload().get(claimKey, Admin.class);
        });
    }


}