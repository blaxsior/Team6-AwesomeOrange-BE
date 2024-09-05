package hyundai.softeer.orange.event.dto;

import hyundai.softeer.orange.event.common.enums.EventType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * 이벤트 리스트를 위한 정보만 담고 있는 객체
 */
@Getter
@Setter
public class BriefEventDto {
    /**
     * HD000000_000 형식으로 구성된 id 값
     */
    private String eventId;
    /**
     * 이벤트의 이름
     */
    private String name;

    /**
     * 이벤트 시작 시간
     */
    private Instant startTime;

    /**
     * 이벤트 종료 시간
     */
    private Instant endTime;

    /**
     * 이벤트의 타입
     */
    private EventType eventType;
}
