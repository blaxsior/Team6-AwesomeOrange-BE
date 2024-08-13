package hyundai.softeer.orange.event.draw.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.event.common.entity.EventMetadata;
import hyundai.softeer.orange.event.common.enums.EventType;
import hyundai.softeer.orange.event.common.exception.EventException;
import hyundai.softeer.orange.event.common.repository.EventMetadataRepository;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDateDto;
import hyundai.softeer.orange.event.draw.dto.EventParticipationDatesDto;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.entity.EventParticipationInfo;
import hyundai.softeer.orange.event.draw.repository.EventParticipationInfoRepository;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.exception.EventUserException;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 이벤트 참여 정보와 관련된 기능을 처리하는 부분
 */
@RequiredArgsConstructor
@Service
public class EventParticipationService {
    private final EventParticipationInfoRepository participationInfoRepository;
    private final EventMetadataRepository emRepository;
    private final EventUserRepository eventUserRepository;

    /**
     * 이벤트 유저가 참여한 이벤트 날짜 목록을 반환한다.
     * @param eventUserId 이벤트 유저의 id
     * @return 이벤트 유저가 추첨 이벤트에 참여한 날짜 목록
     */
    @Transactional(readOnly = true)
    public EventParticipationDatesDto getParticipationDateList(String eventId, String eventUserId) {
        // 이벤트 존재 여부 검증
        EventMetadata event = emRepository.findFirstByEventId(eventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));

        if(event.getEventType() != EventType.draw) throw new EventException(ErrorCode.EVENT_NOT_FOUND);
        DrawEvent drawEvent = event.getDrawEvent();
        if(drawEvent == null) throw new EventException(ErrorCode.EVENT_NOT_FOUND);

        List<EventParticipationDateDto> infos = participationInfoRepository.findByEventUserId(eventUserId, drawEvent.getId());

        return new EventParticipationDatesDto(infos.stream().map(EventParticipationDateDto::getDate).toList());
    }

    /**
     * 오늘의 추첨 이벤트에 참여한다
     * @param eventId 참여하는 이벤트
     * @param eventUserId 이벤트 유저의 id
     */
    @Transactional
    public void participateDaily(String eventId, String eventUserId) {
        participateAtDate(eventId, eventUserId, LocalDateTime.now());
    }

    /**
     * 오늘의 추첨 이벤트에 참여한다
     * @param eventId 참여하는 이벤트
     * @param eventUserId 이벤트 유저의 id
     * @param date 이벤트에 참여하는 날짜.
     */
    protected void participateAtDate(String eventId, String eventUserId, LocalDateTime date) {
        // 이벤트가 존재하는지 검사
        EventMetadata event = emRepository.findFirstByEventId(eventId)
                .orElseThrow(() -> new EventException(ErrorCode.EVENT_NOT_FOUND));

        if(event.getEventType() != EventType.draw) throw new EventException(ErrorCode.EVENT_NOT_FOUND);

        DrawEvent drawEvent = event.getDrawEvent();

        EventUser eventUser = eventUserRepository.findByUserId(eventUserId)
                .orElseThrow(() -> new EventUserException(ErrorCode.USER_NOT_FOUND));

        LocalDate today = date.toLocalDate();

        // 이벤트 기간 안에 있는지 검사
        if(event.getStartTime().isAfter(date) || event.getEndTime().isBefore(date))
            throw new EventException(ErrorCode.INVALID_EVENT_TIME);

        // 오늘 시작 / 끝 시간
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 오늘 참여 여부 검사
        boolean alreadyParticipated = participationInfoRepository.existsByEventUserAndDrawEventAndDateBetween(eventUser, drawEvent, startOfDay, endOfDay);
        if(alreadyParticipated) throw new EventException(ErrorCode.ALREADY_PARTICIPATED);

        EventParticipationInfo info = EventParticipationInfo.of(date, eventUser, drawEvent);
        participationInfoRepository.save(info);
    }
}
