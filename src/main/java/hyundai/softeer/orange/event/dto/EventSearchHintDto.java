package hyundai.softeer.orange.event.dto;


import lombok.Getter;
import lombok.Setter;

/**
 * 관리자가 이벤트 댓글 검색 시 자동완성 영역에 제공되는 데이터
 */
@Getter
@Setter
public class EventSearchHintDto {
    /**
     * 이벤트의 id
     */
    private String eventId;
    /**
     * 이벤트 이름
     */
    private String name;
}
