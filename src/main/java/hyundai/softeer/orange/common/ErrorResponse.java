package hyundai.softeer.orange.common;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class ErrorResponse {

    private String error;
    private String errorMessage;

    public static ErrorResponse from(ErrorCode errorCode) {
        ErrorResponse response = new ErrorResponse();
        response.error = errorCode.name();
        response.errorMessage = errorCode.getErrorCode();
        return response;
    }

    public static ErrorResponse from(String error, String errorMessage) {
        ErrorResponse response = new ErrorResponse();
        response.error = error;
        response.errorMessage = errorMessage;
        return response;
    }
}
