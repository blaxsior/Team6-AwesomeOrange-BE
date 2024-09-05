package hyundai.softeer.orange.admin.controller;

import hyundai.softeer.orange.admin.dto.AdminSignInRequest;
import hyundai.softeer.orange.admin.dto.AdminSignupRequest;
import hyundai.softeer.orange.admin.service.AdminAuthService;
import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.common.dto.TokenDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin auth", description = "어드민 인증에 사용되는 api")
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
@RestController
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    /**
     *
     * @param dto 관리자 유저 로그인을 위한 정보
     * @return 유저 정보
     */
    @Operation(summary = "관리자 유저가 로그인한다.", description = "관리자 유저가 로그인한다.", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    @PostMapping("/signin")
    public TokenDto signIn(@Valid @RequestBody AdminSignInRequest dto) {
        String userToken = adminAuthService.signIn(dto.getUserName(), dto.getPassword());
        return new TokenDto(userToken);
    }


    /**
     * @param dto 관리자 유저 생성을 위한 정보
     */
    @PostMapping("/signup")
    @Operation(summary = "관리자 유저를 생성한다.", description = "관리자 유저를 생성한다. (1명 내부적으로 만든 후 막을 예정)", responses = {
            @ApiResponse(responseCode = "201", description = "관리자 유저 생성"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 유저", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 에러. 백엔드에 알림 요망", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
    })
    public ResponseEntity<Void> signUp(@Valid @RequestBody AdminSignupRequest dto) {
            adminAuthService.signUp(dto.getUserName(), dto.getPassword(), dto.getNickName());
            return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
