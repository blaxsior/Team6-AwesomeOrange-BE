package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.EventConst;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.draw.dto.DrawEventStatusDto;
import hyundai.softeer.orange.event.draw.dto.ResponseDrawWinnerDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.enums.DrawEventStatus;
import hyundai.softeer.orange.event.draw.exception.DrawEventException;
import hyundai.softeer.orange.event.draw.repository.DrawEventWinningInfoRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 추첨 이벤트를 다루는 서비스
 */
@RequiredArgsConstructor
@Service
public class DrawEventService {
    private static final Logger log = LoggerFactory.getLogger(DrawEventService.class);
    private final EventMetadataRepository emRepository;
    private final DrawEventWinningInfoRepository deWinningInfoRepository;
    private final DrawEventDrawMachine machine;
    private final StringRedisTemplate redisTemplate;

    /**
     * eventId에 대한 추첨을 진행하는 메서드
     * @param drawEventId draw event의 id 값.
     */
    @Transactional
    public void draw(String drawEventId) {
        // 이벤트가 존재하는지 검사
        EventMetadata event = emRepository.findFirstByEventId(drawEventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
        DrawEvent drawEvent = event.getDrawEvent();
        if(drawEvent == null) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);
        // 이벤트 검증
        validateDrawCondition(event, Instant.now());
        String key = EventConst.IS_DRAWING(event.getEventId());
        tryDraw(key);

        log.info("Event [{}]: start draw", event.getEventId());
        machine.draw(drawEvent)
        // 시간 제한
        .orTimeout(EventConst.DRAW_EVENT_DRAW_TIMEOUT_HOUR, TimeUnit.HOURS)
        // 예외가 발생하더라도 추첨이 끝나면 키를 제거해야 함 = release
        .handleAsync((unused, throwable) -> {
            releaseDraw(key);
            log.info("Event [{}]: finish draw", event.getEventId());
            if(throwable != null) log.error("Event[{}]", event.getEventId(), throwable);
            return null;
        });
    }

    /**
     * 추첨 이벤트에 당첨된 사용자들을 반환하는 메서드
     * @param drawEventId draw event의 id 값
     */
    @Transactional(readOnly = true)
    public List<ResponseDrawWinnerDto> getDrawEventWinner(String drawEventId) {
        // 이벤트가 존재하는지 검사
        EventMetadata event = emRepository.findFirstByEventId(drawEventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
        DrawEvent drawEvent = event.getDrawEvent();
        if(drawEvent == null) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);

        // 당첨자 목록 반환
        return deWinningInfoRepository.findAllByDrawEventId(drawEvent.getId())
                .stream()
                .map(ResponseDrawWinnerDto::new)
                .toList();
    }

    private void tryDraw(String key) {
        Long count = redisTemplate.opsForValue().increment(key);
        assert count != null; // 트랜잭션이 아니므로 null 이면 안됨.
        if (count > 1) throw new DrawEventException(ErrorCode.EVENT_IS_DRAWING);
        // N 시간동안 유지. 추후 변경될 수 있음
        redisTemplate.expire(key, Duration.ofHours(EventConst.DRAW_EVENT_DRAW_TIMEOUT_HOUR));
    }

    private void releaseDraw(String key) {
        redisTemplate.delete(key);
    }

    private void validateDrawCondition(EventMetadata event, Instant now) {
        // 이벤트가 draw event 인지 검사
        if (event.getEventType() != EventType.draw) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);
        // 이벤트가 종료되었는지 검사
        if (!event.isEnded(now)) throw new DrawEventException(ErrorCode.EVENT_NOT_ENDED);

        // draw 이벤트 객체가 있는지 검사
        DrawEvent drawEvent = event.getDrawEvent();
        if (drawEvent == null) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);
        // 이벤트가 이미 추첨되었는지 검사
        if (drawEvent.isDrawn()) throw new DrawEventException(ErrorCode.ALREADY_DRAWN);
    }

    /**
     * 이벤트의 현재 상태를 얻는다. 이벤트의 상태는 DrawEventStatus에 명시되어 있다.
     * @param eventId 이벤트의 Id
     * @return 추첨 이벤트의 현재 상태
     */
    public DrawEventStatusDto getDrawEventStatus(String eventId) {
        // 이벤트가 존재하는지 검사
        EventMetadata event = emRepository.findFirstByEventId(eventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));
        DrawEvent drawEvent = event.getDrawEvent();
        if(drawEvent == null) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);
        // 이벤트가 draw event 인지 검사
        if (event.getEventType() != EventType.draw) throw new DrawEventException(ErrorCode.EVENT_NOT_FOUND);

        DrawEventStatus status = DrawEventStatus.AVAILABLE;

        Instant now = Instant.now();

        // 이벤트가 종료되었는지 검사
        if (!event.isEnded(now)) status = DrawEventStatus.BEFORE_END;
        else if(drawEvent.isDrawn()) status = DrawEventStatus.COMPLETE;
        // 값이 지정되어 있기만 해도 현재 로직 상 추첨 중
        else {
            String key = EventConst.IS_DRAWING(event.getEventId());
            String value = redisTemplate.opsForValue().get(key);
            if (value != null) status = DrawEventStatus.IS_DRAWING;
        }

        return DrawEventStatusDto.of(event.getEventId(), status);
    }
}
