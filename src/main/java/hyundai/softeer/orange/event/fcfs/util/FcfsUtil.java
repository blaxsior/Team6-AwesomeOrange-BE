package hyundai.softeer.orange.event.fcfs.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcfsUtil {

    private static final String FCFS_PREFIX = "fcfs:";

    // 참여할 선착순 이벤트의 식별자를 간접적으로 보관하는 tag, key는 선착순 이벤트와 연결된 eventMetadata의 eventId이다.
    public static String eventIdFormatting(String key) {
        return FCFS_PREFIX + key + ":eventId";
    }

    // 선착순 이벤트 tag
    public static String keyFormatting(String key) {
        return FCFS_PREFIX + key + ":count";
    }

    // 선착순 이벤트 시작 시각 tag
    public static String startTimeFormatting(String key) {
        return FCFS_PREFIX + key + ":start";
    }

    // 선착순 이벤트 마감 여부 tag
    public static String endFlagFormatting(String key) {
        return FCFS_PREFIX + key + ":end";
    }

    // 선착순 이벤트 당첨자 tag
    public static String winnerFormatting(String key) {
        return FCFS_PREFIX + key + ":winner";
    }

    // 선착순 이벤트 참여자 tag
    public static String participantFormatting(String key) {
        return FCFS_PREFIX + key + ":participant";
    }

    // 선착순 이벤트 정답 tag
    public static String answerFormatting(String key) {
        return FCFS_PREFIX + key + ":answer";
    }
}
