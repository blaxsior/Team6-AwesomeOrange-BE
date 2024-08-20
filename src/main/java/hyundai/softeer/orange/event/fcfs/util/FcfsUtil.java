package hyundai.softeer.orange.event.fcfs.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FcfsUtil {

    // 선착순 이벤트의 PK를 간접적으로 보관하는 eventId tag
    public static String eventIdFormatting(String key) {
        return key + ":eventId";
    }

    // 선착순 이벤트 tag
    public static String keyFormatting(String key) {
        return key + ":fcfs";
    }

    // 선착순 이벤트 시작 시각 tag
    public static String startTimeFormatting(String key) {
        return key + ":start";
    }

    // 선착순 이벤트 마감 여부 tag
    public static String endFlagFormatting(String key) {
        return key + ":end";
    }

    // 선착순 이벤트 당첨자 tag
    public static String winnerFormatting(String key) {
        return key + ":winner";
    }

    // 선착순 이벤트 참여자 tag
    public static String participantFormatting(String key) {
        return key + ":participant";
    }

    // 선착순 이벤트 정답 tag
    public static String answerFormatting(String key) {
        return key + ":answer";
    }
}
