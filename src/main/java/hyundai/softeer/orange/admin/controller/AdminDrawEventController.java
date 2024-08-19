package hyundai.softeer.orange.admin.controller;

import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.event.draw.dto.DrawEventStatusDto;
import hyundai.softeer.orange.event.draw.dto.ResponseDrawWinnerDto;
import hyundai.softeer.orange.core.auth.Auth;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.auth.list.AdminAuthRequirement;
import hyundai.softeer.orange.event.draw.service.DrawEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AdminDrawEvent", description = "어드민 추첨 이벤트 관련 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/draw")
@RestController
@AdminAuthRequirement @Auth({AuthRole.admin})
public class AdminDrawEventController {
    private final DrawEventService drawEventService;
    /**
     * @param eventId 추첨할 이벤트 id
     */
    @Operation(summary = "추첨을 진행한다.", description = "현재 종료된 이벤트의 추첨을 진행한다. 추첨 결과는 기다리지 않는다.", responses = {
            @ApiResponse(responseCode = "200", description = "추첨 성공"),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없는 경우", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("{eventId}/draw")
    public ResponseEntity<Void> drawEvent(@PathVariable("eventId") String eventId) {
        drawEventService.draw(eventId);
        return ResponseEntity.ok().build();
    }

    /**
     * @param eventId 추첨할 이벤트 id
     */
    @Operation(summary = "당첨 유저 목록 조회", description = "특정 이벤트의 추첨 결과 당첨된 총 유저 목록을 조회한다.", responses = {
            @ApiResponse(responseCode = "200", description = "당첨 유저 목록 조회 성공", content = @Content(schema = @Schema(implementation = ResponseDrawWinnerDto.class))),
            @ApiResponse(responseCode = "404", description = "이벤트를 찾을 수 없는 경우", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("{eventId}/winners")
    public ResponseEntity<List<ResponseDrawWinnerDto>> getWinners(@PathVariable("eventId") String eventId) {
        return ResponseEntity.ok(drawEventService.getDrawEventWinner(eventId));
    }


    @Operation(summary = "추첨 이벤트의 현재 상태를 얻는다.", description = "추첨 이벤트의 현재 상태를 얻는다. 추첨 이벤트의 상태는 추첨 불가, 추첨 가능, 추첨 중, 추첨 완료 중 하나의 값을 가진다.", responses = {
            @ApiResponse(responseCode = "200", description = "추첨 이벤트 현재 상태 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이벤트", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @GetMapping("/{eventId}/status")
    public ResponseEntity<DrawEventStatusDto> getEventStatus(
            @PathVariable("eventId") String eventId
    ) {
        DrawEventStatusDto result = drawEventService.getDrawEventStatus(eventId);
        return ResponseEntity.ok(result);
    }
}
