package hyundai.softeer.orange.event.dto.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Constraint(validatedBy = {FcfsEventDtoTimeValidator.class})
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface FcfsEventDtoTimeValidation {
    String message() default "{eventTime.startTimeLaterThanEndTime}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
