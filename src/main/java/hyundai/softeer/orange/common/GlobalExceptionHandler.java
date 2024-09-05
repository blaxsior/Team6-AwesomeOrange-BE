package hyundai.softeer.orange.common;

import hyundai.softeer.orange.admin.exception.AdminException;
import hyundai.softeer.orange.comment.exception.CommentException;

import hyundai.softeer.orange.common.exception.InternalServerException;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.url.exception.UrlException;
import hyundai.softeer.orange.eventuser.exception.EventUserException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @Autowired
    private MessageSource messageSource;

    @ExceptionHandler(MethodArgumentNotValidException.class) // 요청의 유효성 검사 실패 시
    @ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request로 응답 반환
    public Map<String, String> handleInValidRequestException(MethodArgumentNotValidException e) {
        Locale locale = LocaleContextHolder.getLocale();
        // 에러가 발생한 객체 내 필드와 대응하는 에러 메시지를 map에 저장하여 반환
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> {
            String fieldName = error.getField();

            String errorMessage = messageSource.getMessage(error, locale);
            errors.put(fieldName, errorMessage);
        });
        // global error 도 지원
        e.getBindingResult().getGlobalErrors().forEach(error -> {
            String objectName = error.getObjectName();

            String errorMessage = messageSource.getMessage(error, locale);
            errors.put(objectName, errorMessage);
        });

        return errors;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,String> handleInValidRequestException(MethodArgumentTypeMismatchException e) {
        String code = e.getErrorCode();
        String fieldName = e.getName();
        Locale locale = LocaleContextHolder.getLocale(); // 현재 스레드의 로케일 정보를 가져온다.
        String errorMessage = messageSource.getMessage(code, null, code, locale); // 국제화 된 메시지를 가져온다.

        return Map.of(fieldName, errorMessage);
    }

    @ExceptionHandler({BaseException.class})
    public ResponseEntity<ErrorResponse> handleAllBaseException(BaseException e) {
        var code = e.getErrorCode();
        var status = code.getHttpStatus();
        var message = code.getMessage();

        Locale locale = LocaleContextHolder.getLocale(); // 현재 스레드의 로케일 정보를 가져온다.
        String errorMessage = messageSource.getMessage(message, null, message, locale); // 국제화 된 메시지를 가져온다.

        return ResponseEntity
                .status(status)
                .body(ErrorResponse.from(code.name(), errorMessage));
    }
}
