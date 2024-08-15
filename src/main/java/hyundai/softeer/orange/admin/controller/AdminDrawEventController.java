package hyundai.softeer.orange.admin.controller;

import hyundai.softeer.orange.event.draw.service.DrawEventService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/draw")
@RestController
public class AdminDrawEventController {
    private final DrawEventService drawEventService;
    /**
     * @param eventId 추첨할 이벤트 id
     */
    @Operation(summary = "추첨을 진행한다.", description = "현재 종료된 이벤트의 추첨을 진행한다. 추첨 결과는 기다리지 않는다.")
    @PostMapping("{eventId}/draw")
    public ResponseEntity<Void> drawEvent(@PathVariable("eventId") String eventId) {
        drawEventService.draw(eventId);
        return ResponseEntity.ok().build();
    }
}
