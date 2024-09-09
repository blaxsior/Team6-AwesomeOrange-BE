package hyundai.softeer.orange.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public enum ErrorCode {
    // 400 Bad Request
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "bad_request"),
    INVALID_COMMENT(HttpStatus.BAD_REQUEST, "comment.invalid"),
    INVALID_JSON(HttpStatus.BAD_REQUEST, "json.invalid"),
    INVALID_URL(HttpStatus.BAD_REQUEST, "url.invalid"),
    INVALID_INPUT_EVENT_TIME(HttpStatus.BAD_REQUEST, "event.inputEventTime.invalid"),
    INVALID_EVENT_TIME(HttpStatus.BAD_REQUEST, "event.time.invalid"),
    INVALID_EVENT_TYPE(HttpStatus.BAD_REQUEST, "event.type.invalid"),
    EVENT_NOT_ENDED(HttpStatus.BAD_REQUEST, "event.notEnded"),
    EVENT_IS_DRAWING(HttpStatus.BAD_REQUEST, "drawEvent.isDrawing"),
    DUPLICATED_POLICIES(HttpStatus.BAD_REQUEST,"drawEvent.metadata.duplicatedPolicy"),
    DUPLICATED_GRADES(HttpStatus.BAD_REQUEST,"drawEvent.metadata.duplicatedGrade"),
    CANNOT_PARTICIPATE(HttpStatus.BAD_REQUEST,"drawEvent.cannotParticipate"),
    CANNOT_DELETE_EVENT_RUNNING(HttpStatus.BAD_REQUEST,"event.cannotDeleteEventRunning"),
    CANNOT_DELETE_EVENT_ENDED(HttpStatus.BAD_REQUEST,"event.cannotDeleteEventEnded"),


    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "auth.unauthorized"),
    AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "auth.authenticateFailed"),
    INVALID_AUTH_CODE(HttpStatus.UNAUTHORIZED, "auth.invalid.authCode"),
    SESSION_EXPIRED(HttpStatus.UNAUTHORIZED, "auth.session.expired"),
    AUTH_CODE_EXPIRED(HttpStatus.UNAUTHORIZED, "auth.code.expired"),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "forbidden"),

    // 404 Not Found
    SHORT_URL_NOT_FOUND(HttpStatus.NOT_FOUND, "shortUrl.notFound"),
    FCFS_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "fcfsEvent.notFound"),
    DRAW_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "drawEvent.notFound"),
    EVENT_FRAME_NOT_FOUND(HttpStatus.NOT_FOUND, "eventFrame.notFound"),
    EVENT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "eventUser.notFound"),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "comment.notFound"),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "event.notFound"),
    EVENT_NOT_PARTICIPATED(HttpStatus.NOT_FOUND, "event.notParticipated"),
    TEMP_EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "event.temp.notFound"),

    // 405 Method Not Allowed
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "method_not_allowed"),

    // 409 Conflict
    EVENTUSER_ALREADY_EXISTS(HttpStatus.CONFLICT, "eventUser.alreadyExist"),
    SHORT_URL_ALREADY_EXISTS(HttpStatus.CONFLICT, "shortUrl.alreadyExist"),
    COMMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "comment.alreadyExist"),
    ADMIN_USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "admin.alreadyExist"),
    ALREADY_WINNER(HttpStatus.CONFLICT, "event.user.alreadyWon"),
    ALREADY_PARTICIPATED(HttpStatus.CONFLICT, "event.user.alreadyParticipated"),
    PHONE_NUMBER_ALREADY_EXISTS(HttpStatus.CONFLICT, "eventUser.phone_number.alreadyExists"),
    ALREADY_DRAWN(HttpStatus.CONFLICT, "drawEvent.alreadyDrawn"),
    EDIT_TO_DIFFERENT_EVENT_TYPE_NOT_ALLOWED(HttpStatus.CONFLICT,"event.edit_to_different_event_type.not_allowed"),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");

    private final HttpStatus httpStatus;
    private final String errorCode;
}
