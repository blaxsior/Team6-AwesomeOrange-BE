package hyundai.softeer.orange.comment.service;

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

import java.util.Map;

@RequiredArgsConstructor
@Service
public class CommentValidator {

    private static final Logger log = LoggerFactory.getLogger(CommentValidator.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final NaverApiConfig naverApiConfig;
    private final ObjectMapper objectMapper;

    public boolean analyzeComment(String content) {
        String responseBody = sendSentimentAnalysisRequest(content);
        return parseSentimentAnalysisResponse(responseBody, content);
    }

    private String sendSentimentAnalysisRequest(String content) {
        HttpHeaders headers = createHeaders();
        String requestJson = createRequestBody(content);

        HttpEntity<String> requestEntity = new HttpEntity<>(requestJson, headers);
        log.info("comment <{}> sentiment analysis request to Naver API", content);

        ResponseEntity<String> responseEntity = restTemplate.postForEntity(naverApiConfig.getUrl(), requestEntity, String.class);
        return responseEntity.getBody();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(ConstantUtil.CLIENT_ID, naverApiConfig.getClientId());
        headers.set(ConstantUtil.CLIENT_SECRET, naverApiConfig.getClientSecret());
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private String createRequestBody(String content) {
        Map<String, String> requestBody = Map.of("content", content);
        try {
            return objectMapper.writeValueAsString(requestBody);
        } catch (Exception e) {
            throw new CommentException(ErrorCode.INVALID_JSON);
        }
    }

    private boolean parseSentimentAnalysisResponse(String responseBody, String content) {
        try {
            JsonNode rootNode = objectMapper.readTree(responseBody);
            String sentiment = rootNode.path("document").path("sentiment").asText();
            log.info("comment <{}> sentiment analysis result: {}", content, sentiment);

            if (sentiment.equals("negative")) {
                double negativeConfidence = rootNode.path("document").path("confidence").path("negative").asDouble();
                if (negativeConfidence >= ConstantUtil.LIMIT_NEGATIVE_CONFIDENCE) {
                    throw new CommentException(ErrorCode.INVALID_COMMENT);
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new CommentException(ErrorCode.INVALID_JSON);
        }
    }
}
