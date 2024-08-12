package hyundai.softeer.orange.event.common;

import java.util.Set;

public class EventConst {
    public static final String REDIS_KEY_PREFIX = "@event_key:";

    public static final String REDIS_TEMP_EVENT_PREFIX = REDIS_KEY_PREFIX + "temp:";
    public static String TEMP_KEY(Long userId) {
        return REDIS_TEMP_EVENT_PREFIX + userId;
    }
    public static final long TEMP_EVENT_DURATION_HOUR = 24L;

    // 검색 기능 관련 상수들
    public static final int EVENT_DEFAULT_PAGE = 0;
    public static final int EVENT_DEFAULT_SIZE = 5;
    public static final Set<String> sortableFields = Set.of("eventId", "name", "startTime", "endTime", "eventType");
}
