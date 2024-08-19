package hyundai.softeer.orange.comment.service;

import hyundai.softeer.orange.comment.dto.CreateCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentsDto;
import hyundai.softeer.orange.comment.entity.Comment;
import hyundai.softeer.orange.comment.exception.CommentException;
import hyundai.softeer.orange.comment.repository.CommentRepository;
import hyundai.softeer.orange.comment.repository.CommentSpecification;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.event.draw.entity.DrawEvent;
import hyundai.softeer.orange.event.draw.repository.DrawEventRepository;
import hyundai.softeer.orange.event.draw.repository.EventParticipationInfoRepository;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private final CommentRepository commentRepository;
    private final EventFrameRepository eventFrameRepository;
    private final EventUserRepository eventUserRepository;
    private final EventParticipationInfoRepository participationInfoRepository;
    private final DrawEventRepository drawEventRepository;
    private final CommentValidator commentValidator;

    // 주기적으로 무작위 추출되는 긍정 기대평 목록을 조회한다.
    @Transactional(readOnly = true)
    @Cacheable(value = "comments", key = ConstantUtil.COMMENTS_KEY + " + #eventFrameId")
    public ResponseCommentsDto getComments(String eventFrameId) {
        log.info("fetching comments of {}", eventFrameId);
        EventFrame frame = getEventFrame(eventFrameId);
        List<ResponseCommentDto> comments = commentRepository.findRandomPositiveComments(frame.getId(), PageRequest.of(0, ConstantUtil.COMMENTS_SIZE));
        log.info("comments of {} fetched from DB to Redis", eventFrameId);
        return new ResponseCommentsDto(comments);
    }

    // 신규 기대평을 등록한다.
    @Transactional
    public Boolean createComment(String userId, String eventFrameId, CreateCommentDto dto) {
        EventUser eventUser = getEventUser(userId);
        EventFrame eventFrame = getEventFrame(eventFrameId);
        DrawEvent drawEvent = drawEventRepository.findByEventFrameId(eventFrameId)
                .orElseThrow(() -> new CommentException(ErrorCode.DRAW_EVENT_NOT_FOUND));

        // 오늘의 시작과 끝 시각 계산
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        // 오늘 유저가 인터렉션에 참여하지 않았다면 예외처리
        boolean participated = participationInfoRepository.existsByEventUserAndDrawEventAndDateBetween(eventUser, drawEvent, startOfDay, endOfDay);
        if(!participated) throw new CommentException(ErrorCode.EVENT_NOT_PARTICIPATED);

        // 하루에 여러 번의 기대평을 작성하려 할 때 예외처리
        if(commentRepository.existsByCreatedDateAndEventUser(eventUser.getId())) {
            throw new CommentException(ErrorCode.COMMENT_ALREADY_EXISTS);
        }

        boolean isPositive = commentValidator.analyzeComment(dto.getContent());

        Comment comment = Comment.of(dto.getContent(), eventFrame, eventUser, isPositive);
        commentRepository.save(comment);
        log.info("created comment: {}", comment.getId());
        return true;
    }

    // 오늘 이 유저가 기대평을 작성할 수 있는지 여부를 조회한다.
    @Transactional(readOnly = true)
    public Boolean isCommentable(String userId) {
        EventUser eventUser = getEventUser(userId);
        log.info("checking commentable of user {}", eventUser.getUserId());
        return !commentRepository.existsByCreatedDateAndEventUser(eventUser.getId());
    }

    // 기대평을 삭제한다. 이 동작을 실행하는 주체가 어드민임이 반드시 검증되어야 한다.
    @Transactional
    public Long deleteComment(Long commentId) {
        if(!commentRepository.existsById(commentId)) {
            throw new CommentException(ErrorCode.COMMENT_NOT_FOUND);
        }

        commentRepository.deleteById(commentId);
        log.info("deleted comment: {}", commentId);
        return commentId;
    }

    @Transactional
    @CacheEvict(value = "comments", allEntries = true)
    public void deleteComments(List<Long> commentIds) {
        commentRepository.deleteAllById(commentIds);
        log.info("deleted comments: {}", commentIds);
    }

    @Transactional(readOnly = true)
    public ResponseCommentsDto searchComments(String eventId, String search, Integer page, Integer size) {
        PageRequest pageInfo = PageRequest.of(page, size);

        Specification<Comment> matchEventId = CommentSpecification.matchEventId(eventId);
        Specification<Comment> searchOnContent = CommentSpecification.searchOnContent(search);

        var comments = commentRepository.findAll(
                matchEventId.and(searchOnContent),
                pageInfo
        ).stream().map(ResponseCommentDto::from).toList();

        log.info("searched comments: {}", comments);
        return new ResponseCommentsDto(comments);
    }

    private EventUser getEventUser(String userId) {
        return eventUserRepository.findByUserId(userId)
                .orElseThrow(() -> new CommentException(ErrorCode.EVENT_USER_NOT_FOUND));
    }

    private EventFrame getEventFrame(String eventFrameId) {
        return eventFrameRepository.findByFrameId(eventFrameId)
                .orElseThrow(() -> new CommentException(ErrorCode.EVENT_FRAME_NOT_FOUND));
    }
}
