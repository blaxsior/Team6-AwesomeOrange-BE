package hyundai.softeer.orange.comment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import hyundai.softeer.orange.comment.exception.CommentException;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.config.NaverApiConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class CommentValidator {

    private static final Logger log = LoggerFactory.getLogger(CommentValidator.class);
    private final NaverApiConfig naverApiConfig;

    public boolean analyzeComment(String content) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ConstantUtil.CLIENT_ID, naverApiConfig.getClientId());
        headers.set(ConstantUtil.CLIENT_SECRET, naverApiConfig.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Create a JSON for the request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("content", content);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonContent = "";
        try {
            jsonContent = objectMapper.writeValueAsString(requestBody);
        } catch (JsonProcessingException e) {
            throw new CommentException(ErrorCode.INVALID_JSON);
        }

        HttpEntity<String> requestEntity = new HttpEntity<>(jsonContent, headers);
        log.info("comment <{}> sentiment analysis request to Naver API", content);

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.postForEntity(naverApiConfig.getUrl(), requestEntity, String.class);
        String responseBody = responseEntity.getBody();
        boolean isPositive = true;

        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);

            String sentiment = rootNode.path("document").path("sentiment").asText();
            if (sentiment.equals("negative")) {
                isPositive = false;
                double documentNegativeConfidence = rootNode.path("document").path("confidence").path("negative").asDouble();
                if (documentNegativeConfidence >= ConstantUtil.LIMIT_NEGATIVE_CONFIDENCE) { // 부정이며 확률이 99.5% 이상일 경우 재작성 요청
                    throw new CommentException(ErrorCode.INVALID_COMMENT);
                }
            }
        } catch (JsonProcessingException e) {
            throw new CommentException(ErrorCode.INVALID_JSON);
        }
        log.info("comment <{}> sentiment analysis result: {}", content, isPositive);
        return isPositive;
    }
}
