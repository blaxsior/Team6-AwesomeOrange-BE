package hyundai.softeer.orange.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import hyundai.softeer.orange.common.exception.InternalServerException;
import lombok.RequiredArgsConstructor;

// TODO: 더 좋은 클래스 이름 / 일반화 방법 고민하기.
@RequiredArgsConstructor
public class ParseUtil {
    private final ObjectMapper objectMapper;

    public <T> T parse(String content, Class<T> clazz) {
        try {
            return objectMapper.readValue(content, clazz);
        } catch (Exception e) {
            throw new InternalServerException();
        }
    }
}
