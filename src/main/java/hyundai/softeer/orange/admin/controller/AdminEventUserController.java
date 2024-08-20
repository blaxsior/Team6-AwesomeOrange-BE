package hyundai.softeer.orange.admin.controller;

import hyundai.softeer.orange.core.auth.Auth;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.auth.list.AdminAuthRequirement;
import hyundai.softeer.orange.eventuser.dto.EventUserPageDto;
import hyundai.softeer.orange.eventuser.service.EventUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name="admin event user", description = "어드민 페이지에서 이벤트 유저 정보 조회에 사용하는 API")
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/event-users")
@RestController
@AdminAuthRequirement @Auth({AuthRole.admin})
public class AdminEventUserController {
    private final EventUserService eventUserService;

    /**
     * @param search 이벤트 유저 이름 검색어
     * @param page 현재 페이지
     * @param size 한 페이지의 크기
     */
    @Operation(summary = "이벤트 유저 목록을 검색한다", description="이벤트 유저 목록을 검색한다. 이름을 기준으로 검색할 수 있으며, 페이지 / 사이즈가 존재한다.", responses = {
            @ApiResponse(responseCode = "200", description = "매칭되는 이벤트 유저 리스트를 반환한다."),
    })
    @GetMapping
    public ResponseEntity<EventUserPageDto> getEventUsers(
            @RequestParam(name="search", required = false, defaultValue = "") String search,
            @RequestParam(name="page", required = false, defaultValue = "0") @Min(0) Integer page,
            @RequestParam(name="size", required = false, defaultValue = "10") @Min(1) Integer size
    ) {
        var userPageDto = eventUserService.getUserBySearch(search, page, size);
        return ResponseEntity.ok(userPageDto);
    }

}
