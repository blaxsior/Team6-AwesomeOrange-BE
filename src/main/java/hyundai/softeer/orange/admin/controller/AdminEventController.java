package hyundai.softeer.orange.admin.controller;

import hyundai.softeer.orange.admin.component.AdminAnnotation;
import hyundai.softeer.orange.admin.dto.AdminDto;
import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.core.auth.Auth;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.auth.list.AdminAuthRequirement;
import hyundai.softeer.orange.event.common.service.EventService;
import hyundai.softeer.orange.event.dto.*;
import hyundai.softeer.orange.event.dto.group.EventEditGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이벤트 관련 CRUD를 다루는 API
 */
@Tag(name = "admin event", description = "어드민 페이지에서 이벤트 관리에 사용하는 api")
@Slf4j
@AdminAuthRequirement @Auth({AuthRole.admin})
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/events")
@RestController
public class AdminEventController {
    private final EventService eventService;
    /**
     *
     * @param search 검색어
     * @param sort 정렬 기준. (eventId|name|startTime|endTime|eventType)(:(asc|desc))? 패턴이 ,로 나뉘는 형태. ex) eventId,name:asc,startTime:desc
     * @param type 선택할 이벤트의 타입 (fcfs|draw)을 ,로 나눠 표현. ex) type=fcfs,draw / 비어 있으면 모두 선택
     * @param page 페이지 번호
     * @param size 한번에 검색하는 이벤트 개수
     * @return 요청한 이벤트 리스트 및 페이지 정보
     */
    @GetMapping
    @Operation(summary = "이벤트 리스트 획득", description = "관리자가 이벤트 목록을 검색한다. 검색어, sort 기준 등을 정의할 수 있다.", responses = {
            @ApiResponse(responseCode = "200", description = "성공적으로 이벤트 목록을 반환한다"),
            @ApiResponse(responseCode = "5xx", description = "서버 내부적 에러"),
            @ApiResponse(responseCode = "4xx", description = "클라이언트 에러 (보통 page / size 값을 잘못 지정. 숫자가 아닌 경우 등) ")
    })
    public ResponseEntity<BriefEventPageDto> getEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        BriefEventPageDto pageDto = eventService.searchEvents(search, sort, type, page, size);
        return ResponseEntity.ok(pageDto);
    }

    @PostMapping
    @Operation(summary = "이벤트 생성", description = "관리자가 이벤트를 새롭게 등록한다", responses = {
            @ApiResponse(responseCode = "201", description = "이벤트 생성 성공"),
            @ApiResponse(responseCode = "4xx", description = "유저 측 실수로 이벤트 생성 실패")
    })
    public ResponseEntity<Void> createEvent(@Validated @RequestBody EventDto eventDto,
                                            @Parameter(hidden = true) @AdminAnnotation AdminDto admin
    ) {
        // 나중에 두개 과정을 통합할 수도 있음.
        eventService.createEvent(eventDto);
        eventService.clearTempEvent(admin.getId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     *
     * @param eventId 이벤트 ID. HD000000~로 시작하는 그것
     * @return 해당 이벤트에 대한 정보
     */
    @GetMapping("{eventId}")
    @Operation(summary = "이벤트 데이터 획득", description = "이벤트 초기 정보를 받는다", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 정보를 정상적으로 받음"),
            @ApiResponse(responseCode = "404", description = "대응되는 이벤트가 존재하지 않음")
    })
    public ResponseEntity<EventDto> getEventData(
            @PathVariable("eventId") String eventId
    ) {
        EventDto eventInfo = eventService.getEventInfo(eventId);
        return ResponseEntity.ok(eventInfo);
    }

    /**
     * @param eventDto 수정된 이벤트 정보
     */
    @PostMapping("/edit")
    @Operation(summary = "이벤트 수정", description = "관리자가 이벤트를 수정한다", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 생성 성공"),
            @ApiResponse(responseCode = "4xx", description = "유저 측 실수로 이벤트 생성 실패")
    })
    public ResponseEntity<Void> editEvent(
            @Validated({EventEditGroup.class}) @RequestBody EventDto eventDto) {
        eventService.editEvent(eventDto);
        return ResponseEntity.ok().build();
    }

    /**
     * @param req 이벤트 프레임 생성을 위한 json
     */
    @PostMapping("/frame")
    @Operation(summary = "이벤트 프레임 생성", description = "관리자가 이벤트 프레임을 새롭게 등록한다", responses = {
            @ApiResponse(responseCode = "201", description = "이벤트 프레임 생성 성공"),
            @ApiResponse(responseCode = "4xx", description = "이벤트 프레임 생성 실패")
    })
    public ResponseEntity<Void> createEventFrame(@Valid @RequestBody EventFrameCreateRequest req) {
        eventService.createEventFrame(req.getFrameId(), req.getName());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/hints")
    @Operation(summary="이벤트 힌트 목록 얻기", description = "관리자가 이벤트 댓글 열람을 위해 검색할 때 반환하는 (이벤트 id / 이름 ) 정보 목록을 얻는다.", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 힌트 목록 획득")
    })
    public ResponseEntity<List<EventSearchHintDto>> findEventSearchHints(@RequestParam("search") String search) {
        var searchHints = eventService.searchHints(search);
        return ResponseEntity.ok(searchHints);
    }

    @GetMapping("/temp")
    @Operation(summary = "임시 저장 된 이벤트 불러오기", description = "관리자가 이벤트 프레임을 새롭게 등록한다", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 불러오기 성공"),
    })
    public ResponseEntity<EventDto> getTempEvent(@Parameter(hidden = true) @AdminAnnotation AdminDto admin) {
        Long adminId = admin.getId();
        EventDto eventDto = eventService.getTempEvent(adminId);
        return ResponseEntity.ok(eventDto);
    }

    /**
     * @param eventDto 임시로 저장할 이벤트 정보
     */
    @PostMapping("/temp")
    @Operation(summary = "이벤트 임시 저장", description = "이벤트를 임시 저장한다.", responses = {
            @ApiResponse(responseCode = "200", description = "이벤트 임시 저장 성공"),
            @ApiResponse(responseCode = "4xx", description = "이벤트 임시 저장 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),

    })
    public ResponseEntity<Void> saveTempEvent(@RequestBody EventDto eventDto,
                                              @Parameter(hidden = true) @AdminAnnotation AdminDto admin) {
        Long adminId = admin.getId();
        eventService.saveTempEvent(adminId, eventDto);
        return ResponseEntity.ok().build();
    }
}
