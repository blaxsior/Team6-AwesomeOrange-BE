package hyundai.softeer.orange.admin.controller;

import lombok.Getter;

import java.util.List;

@Getter
public class EventFrameIdListDto {
    private List<String> frameIds;

    public static EventFrameIdListDto of(List<String> frameIds) {
        EventFrameIdListDto dto = new EventFrameIdListDto();
        dto.frameIds = frameIds;
        return dto;
    }
}
