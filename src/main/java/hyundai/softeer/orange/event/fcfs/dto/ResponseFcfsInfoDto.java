package hyundai.softeer.orange.event.fcfs.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResponseFcfsInfoDto {

    private Instant eventStartTime;

    private String eventStatus;
}
