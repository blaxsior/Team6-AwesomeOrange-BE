package hyundai.softeer.orange.event.fcfs.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcfsUtil {

    private static final String FCFS_PREFIX = "fcfs:";

    public static String eventIdFormatting(String key) {
        return formatKey(key, "eventId");
    }

    public static String keyFormatting(String key) {
        return formatKey(key, "count");
    }

    public static String startTimeFormatting(String key) {
        return formatKey(key, "start");
    }

    public static String endFlagFormatting(String key) {
        return formatKey(key, "end");
    }

    public static String winnerFormatting(String key) {
        return formatKey(key, "winner");
    }

    public static String participantFormatting(String key) {
        return formatKey(key, "participant");
    }

    public static String answerFormatting(String key) {
        return formatKey(key, "answer");
    }

    // 공통 로직을 처리하는 메서드
    private static String formatKey(String key, String suffix) {
        return FCFS_PREFIX + key + ":" + suffix;
    }
}
