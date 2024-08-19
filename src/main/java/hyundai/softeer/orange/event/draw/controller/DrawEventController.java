package hyundai.softeer.orange.event.draw.controller;

import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.core.auth.Auth;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.auth.list.EventUserAuthRequirement;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDatesDto;
import hyundai.softeer.orange.event.draw.service.EventParticipationService;
import hyundai.softeer.orange.eventuser.component.EventUserAnnotation;
import hyundai.softeer.orange.eventuser.dto.EventUserInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "DrawEvent", description = "인터렉션 및 추첨 이벤트 관련 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/event/draw")
@RestController
@EventUserAuthRequirement @Auth({AuthRole.event_user})
public class DrawEventController {
    private final EventParticipationService epService;
    /**
     *
     * @param eventId 이벤트의 id
     */
    @Operation(summary = "오늘의 인터렉션 이벤트에 참여한다.", description = "오늘의 인터렉션 이벤트에 참여한다. 서버 시간 기준으로 참여를 정하며, 당일 중복 참여는 불가능하다.", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 참여 기록 획득"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 이벤트에 참여함", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/{eventId}/participation")
    public ResponseEntity<Void> participateDailyEvent(
            @PathVariable ("eventId") String eventId,
            @Parameter(hidden = true) @EventUserAnnotation EventUserInfo userInfo
    ) {
        epService.participateDaily(eventId, userInfo.getUserId());

        return ResponseEntity.ok().build();
    }

    /**
     *
     * @param eventId 이벤트의 id
     * @return 유저의 이벤트 참여 정보
     */
    @Operation(summary = "이벤트 참여일자 목록을 얻는다.", description = "이벤트 유저의 대상 이벤트 참여일자 목록을 얻는다.", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 참여 기록 획득"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{eventId}/participation")
    public ResponseEntity<EventParticipationDatesDto> getParticipationDates(
            @PathVariable ("eventId") String eventId,
            @Parameter(hidden = true) @EventUserAnnotation EventUserInfo userInfo
    ) {
        EventParticipationDatesDto dto = epService.getParticipationDateList(eventId, userInfo.getUserId());
        return ResponseEntity.ok(dto);
    }
}
