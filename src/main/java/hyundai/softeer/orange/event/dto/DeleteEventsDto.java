package hyundai.softeer.orange.event.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class DeleteEventsDto {
    @NotEmpty
    List<String> eventIds;
}
