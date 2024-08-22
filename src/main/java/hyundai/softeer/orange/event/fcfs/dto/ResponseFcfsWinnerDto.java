package hyundai.softeer.orange.event.fcfs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class ResponseFcfsWinnerDto {

    private String name;
    private String phoneNumber;
    private Instant winningTime;
}
