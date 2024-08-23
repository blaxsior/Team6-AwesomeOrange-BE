package hyundai.softeer.orange.common.util;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConstantUtil {

    public static final String COMMENTS_KEY = "'comments'";
    public static final String CLIENT_ID = "X-NCP-APIGW-API-KEY-ID";
    public static final String CLIENT_SECRET = "X-NCP-APIGW-API-KEY";// 2시간
    public static final String PHONE_NUMBER_REGEX = "010\\d{8}"; // 010 + 8자리 숫자
    public static final String AUTH_CODE_REGEX = "\\d{6}"; // 6자리 숫자
    public static final String AUTH_CODE_CREATE_REGEX = "%06d";
    public static final String CLAIMS_USER_KEY = "userId";
    public static final String CLAIMS_ADMIN = "admin";
    public static final String CLAIMS_USER_NAME_KEY = "userName";
    public static final String JWT_USER_KEY = "eventUser";
    public static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    public static final String LOCATION = "Location";
    public static final String COUNTDOWN = "countdown";
    public static final String WAITING = "waiting";
    public static final String PROGRESS = "progress";

    public static final String DB_TO_REDIS_LOCK = "FCFS_MANAGE_DB_TO_REDIS";
    public static final String REDIS_TO_DB_LOCK = "FCFS_MANAGE_REDIS_TO_DB";

    public static final double LIMIT_NEGATIVE_CONFIDENCE = 80;
    public static final int COMMENTS_SIZE = 20;
    public static final int SCHEDULED_TIME = 1000 * 60 * 60 * 2;
    public static final int SHORT_URL_LENGTH = 10;
    public static final int USER_ID_LENGTH = 8;
    public static final int AUTH_CODE_LENGTH = 6;
    public static final int JWT_LIFESPAN = 1; // 1시간
    public static final int AUTH_CODE_EXPIRE_TIME = 5; // 5분
    public static final int FCFS_AVAILABLE_HOUR = 7;
    public static final int FCFS_COUNTDOWN_HOUR = 3;
}
