package hyundai.softeer.orange.core.auth;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.core.jwt.JWTConst;
import hyundai.softeer.orange.core.jwt.JWTManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.*;

@Slf4j
@Component // 문제 있으면 변경
public class AuthInterceptor implements HandlerInterceptor {
    private final JWTManager jwtManager;

    public AuthInterceptor(JWTManager jwtManager) {
        this.jwtManager = jwtManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 유효하지 않은 요청은 정적 리소스로 간주하여 ResourceHttpRequestHandler가 대신 처리하기에, HandlerMethod가 아닌 경우는 무시
        if (!(handler instanceof HandlerMethod handlerMethod)) return true;

        Auth authAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Auth.class);
        if (authAnnotation == null) {
            authAnnotation = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Auth.class);
        }
        // 어노테이션 없으면 인증 필요 없음
        if (authAnnotation == null) return true;

        Set<String> authRoleStringSet = new HashSet<>();

        for (AuthRole role : authAnnotation.value()) {
            authRoleStringSet.add(role.name());
        }

        // 헤더 분석 과정
        String authorizationHeader = request.getHeader("Authorization");

        // 헤더가 없는 경우 => 인증 안됨
        if (authorizationHeader == null) throw new AuthException(ErrorCode.UNAUTHORIZED);
        String[] tokenInfo = authorizationHeader.split("\\s+");

        // Bearer token 형식이 아님 => 인증 안됨
        if (tokenInfo.length < 2 || !tokenInfo[0].equalsIgnoreCase("bearer"))
            throw new AuthException(ErrorCode.UNAUTHORIZED);

        String token = tokenInfo[1];

        try {
            var parsedToken = jwtManager.parseToken(token);
            // 현재 토큰의 역할이 Auth에 정의되어 있는지 검사
            String role = parsedToken.getPayload().get(JWTConst.ROLE, String.class);
            if (!authRoleStringSet.contains(role)) throw new AuthException(ErrorCode.UNAUTHORIZED);

            request.setAttribute(JWTConst.Token, parsedToken);
        } catch (Exception e) {
            throw new AuthException(ErrorCode.UNAUTHORIZED);
        }

        return true;
    }
}
