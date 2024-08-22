package hyundai.softeer.orange.event.component;

import hyundai.softeer.orange.common.util.DateUtil;
import hyundai.softeer.orange.event.common.EventConst;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Component
public class EventKeyGenerator {
    private final DateTimeFormatter formatter;
    private final StringRedisTemplate redisTemplate;

    public EventKeyGenerator(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.formatter = DateTimeFormatter.ofPattern("yyMMdd");
    }

    public String generate() {
        return generate(LocalDateTime.now());
    }

    public String generate(LocalDateTime now) {
        // UTC 기준으로 시차는 -12 ~ 12시간, 키가 48시간 유지되면 모든 지역에서 "오늘"의 키 사용 가능. 여유 시간 10초
        LocalDateTime diffDay = now.plusDays(2).toLocalDate().atTime(0,0,10);

        String dateInfo = formatter.format(now);
        String incKey = EventConst.REDIS_KEY_PREFIX + dateInfo;

        Long number = redisTemplate.opsForValue().increment(incKey);

        if(number != null && number == 1)
            redisTemplate.expireAt(incKey, DateUtil.localDateTimeToDate(diffDay));

        return String.format("HD_%s_%03d", dateInfo, number);
    }
}
