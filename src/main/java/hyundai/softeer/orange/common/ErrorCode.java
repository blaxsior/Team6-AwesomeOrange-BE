package hyundai.softeer.orange.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ErrorCode {
    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_COMMENT(HttpStatus.BAD_REQUEST, "부정적인 표현을 사용하였습니다."),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "잘못된 JSON 형식입니다."),
    INVALID_URL(HttpStatus.BAD_REQUEST, "유효하지 않은 URL입니다."),
    INVALID_INPUT_EVENT_TIME(HttpStatus.BAD_REQUEST, "입력된 시간 중 일부가 조건에 맞지 않습니다."),
    INVALID_EVENT_TIME(HttpStatus.BAD_REQUEST, "이벤트 시간이 아닙니다."),
    INVALID_EVENT_TYPE(HttpStatus.BAD_REQUEST, "이벤트 타입이 지원되지 않습니다."),
    EVENT_NOT_ENDED(HttpStatus.BAD_REQUEST, "이벤트가 아직 종료되지 않았습니다."),
    EVENT_IS_DRAWING(HttpStatus.BAD_REQUEST, "현재 추첨이 진행되고 있는 이벤트입니다."),
    DUPLICATED_POLICIES(HttpStatus.BAD_REQUEST,"정책에서 중복된 액션이 존재합니다."),
    DUPLICATED_GRADES(HttpStatus.BAD_REQUEST,"추첨 이벤트 정보에 중복된 등수가 존재합니다."),
    CANNOT_PARTICIPATE(HttpStatus.BAD_REQUEST,"현재 유저가 참여할 수 없는 이벤트입니다."),
    CANNOT_DELETE_EVENT_RUNNING(HttpStatus.BAD_REQUEST,"이벤트가 진행 중이므로 삭제할 수 없습니다"),
    CANNOT_DELETE_EVENT_ENDED(HttpStatus.BAD_REQUEST,"이벤트가 종료되어 삭제할 수 없습니다"),


    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다."),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다"),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "인증번호가 일치하지 않습니다."),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다."),
    AUTH_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "인증번호가 만료되었거나 존재하지 않습니다."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    SHORT_URL_NOT_FOUND(HttpStatus.NOT_FOUND, "단축 URL을 찾을 수 없습니다."),
    FCFS_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "선착순 이벤트를 찾을 수 없습니다."),
    DRAW_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "추첨 이벤트를 찾을 수 없습니다."),
    EVENT_FRAME_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 프레임을 찾을 수 없습니다."),
    EVENT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트 사용자를 찾을 수 없습니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "기대평을 찾을 수 없습니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "이벤트를 찾을 수 없습니다."),
    EVENT_NOT_PARTICIPATED(HttpStatus.NOT_FOUND, "이벤트에 참여하지 않았습니다."),
    TEMP_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "임시 저장 된 이벤트가 없습니다."),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),

    // 409 Conflict
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 사용자입니다."),
    SHORT_URL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 URL입니다."),
    COMMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 기대평입니다."),
    ADMIN_USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 관리자입니다."),
    ALREADY_WINNER(HttpStatus.CONFLICT, "이미 당첨된 사용자입니다."),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "이미 참여한 사용자입니다."),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 존재하는 전화번호입니다."),
    ALREADY_DRAWN(HttpStatus.CONFLICT, "이미 추첨된 이벤트입니다."),
    EDIT_TO_DIFFERENT_EVENT_TYPE_NOT_ALLOWED(HttpStatus.CONFLICT,"생성된 이벤트를 다른 타입으로 수정할 수 없습니다"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 오류가 발생하였습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
