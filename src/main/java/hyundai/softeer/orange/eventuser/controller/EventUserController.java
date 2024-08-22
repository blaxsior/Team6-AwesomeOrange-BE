package hyundai.softeer.orange.eventuser.controller;

import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.common.dto.TokenDto;
import hyundai.softeer.orange.eventuser.dto.RequestAuthCodeDto;
import hyundai.softeer.orange.eventuser.dto.RequestUserDto;
import hyundai.softeer.orange.eventuser.service.EventUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "EventUser", description = "EventUser 관련 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/event-user")
@RestController
public class EventUserController {

    private final EventUserService eventUserService;

    // 로그인
    @Tag(name = "EventUser")
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "유저의 정보를 입력받아 로그인한다.", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = TokenDto.class))),
            @ApiResponse(responseCode = "400", description = "입력받은 정보의 유효성 검사가 실패했을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 정보를 갖는 유저가 존재하지 않을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<TokenDto> login(@RequestBody @Valid RequestUserDto dto) {
        return ResponseEntity.ok(eventUserService.login(dto));
    }

    // 인증번호 전송
    @Tag(name = "EventUser")
    @PostMapping("/send-auth/{eventFrameId}")
    @Operation(summary = "인증번호 전송", description = "유저의 전화번호에 인증번호를 전송한다.", responses = {
            @ApiResponse(responseCode = "200", description = "인증번호 전송 성공"),
            @ApiResponse(responseCode = "400", description = "입력받은 정보의 유효성 검사가 실패했을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "해당 이벤트 프레임이 존재하지 않을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "해당 이벤트의 유저로 이미 가입되어 있을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> sendAuthCode(@RequestBody @Valid RequestUserDto dto, @PathVariable String eventFrameId) {
        eventUserService.sendAuthCode(dto, eventFrameId);
        return ResponseEntity.ok().build();
    }

    // 인증번호 검증
    @Tag(name = "EventUser")
    @PostMapping("/check-auth/{eventFrameId}")
    @Operation(summary = "인증번호 검증", description = "유저가 입력한 인증번호를 검증한다.", responses = {
            @ApiResponse(responseCode = "200", description = "인증번호 검증 성공",
                    content = @Content(schema = @Schema(implementation = TokenDto.class))),
            @ApiResponse(responseCode = "400", description = "입력받은 정보의 유효성 검사가 실패했을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증번호가 일치하지 않을 때",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public ResponseEntity<TokenDto> checkAuthCode(@PathVariable String eventFrameId,  @RequestBody @Valid RequestAuthCodeDto dto) {
        return ResponseEntity.ok(eventUserService.checkAuthCode(dto, eventFrameId));
    }
}
