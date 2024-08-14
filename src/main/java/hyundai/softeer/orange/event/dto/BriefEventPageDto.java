package hyundai.softeer.orange.event.dto;

import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
public class BriefEventPageDto {
    private List<BriefEventDto> contents;
    /**
     * 총 페이지 수
     */
    private int totalPages;
    /**
     * 현재 페이지 번호
     */
    private int number;
    /**
     * 페이지의 크기
     */
    private int size;

    public static BriefEventPageDto from(Page<BriefEventDto> page) {
        BriefEventPageDto dto = new BriefEventPageDto();
        dto.totalPages = page.getTotalPages();
        dto.number = page.getNumber();
        dto.size = page.getSize();
        dto.contents = page.getContent();
        return dto;
    }
}
