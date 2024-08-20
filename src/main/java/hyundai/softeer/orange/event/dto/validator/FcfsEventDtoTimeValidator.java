package hyundai.softeer.orange.event.dto.validator;

import hyundai.softeer.orange.event.dto.fcfs.FcfsEventDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FcfsEventDtoTimeValidator implements ConstraintValidator<FcfsEventDtoTimeValidation, FcfsEventDto> {
    @Override
    public void initialize(FcfsEventDtoTimeValidation constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(FcfsEventDto dto, ConstraintValidatorContext constraintValidatorContext) {
        var startTime = dto.getStartTime();
        var endTime = dto.getEndTime();
        if(startTime == null || endTime == null) return false;

        return startTime.isBefore(endTime);
    }
}
