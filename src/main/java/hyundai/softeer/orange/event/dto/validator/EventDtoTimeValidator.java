package hyundai.softeer.orange.event.dto.validator;

import hyundai.softeer.orange.event.dto.EventDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EventDtoTimeValidator implements ConstraintValidator<EventDtoTimeValidation, EventDto> {
    @Override
    public void initialize(EventDtoTimeValidation constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(EventDto eventDto, ConstraintValidatorContext constraintValidatorContext) {
        var startTime = eventDto.getStartTime();
        var endTime = eventDto.getEndTime();
        if (startTime == null || endTime == null) return false;

        return startTime.isBefore(endTime);
    }
}
