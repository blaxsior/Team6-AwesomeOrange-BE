package hyundai.softeer.orange.comment.service;

import hyundai.softeer.orange.comment.dto.CreateCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentsDto;
import hyundai.softeer.orange.comment.entity.Comment;
import hyundai.softeer.orange.comment.exception.CommentException;
import hyundai.softeer.orange.comment.repository.CommentRepository;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CommentService {
    private static final Logger log = LoggerFactory.getLogger(CommentService.class);
    private final CommentRepository commentRepository;
    private final EventFrameRepository eventFrameRepository;
    private final EventUserRepository eventUserRepository;
    private final CommentValidator commentValidator;

    // 주기적으로 무작위 추출되는 긍정 기대평 목록을 조회한다.
    @Transactional(readOnly = true)
    @Cacheable(value = "comments", key = ConstantUtil.COMMENTS_KEY + " + #eventFrameId")
    public ResponseCommentsDto getComments(String eventFrameId) {
        EventFrame frame = getEventFrame(eventFrameId);
        List<ResponseCommentDto> comments = commentRepository.findRandomPositiveComments(frame.getId(), ConstantUtil.COMMENTS_SIZE)
                .stream()
                .map(ResponseCommentDto::from)
                .toList();
        log.info("comments of {} fetched from DB to Redis", eventFrameId);
        return new ResponseCommentsDto(comments);
    }

    // 신규 기대평을 등록한다.
    @Transactional
    public Boolean createComment(String userId, String eventFrameId, CreateCommentDto dto) {
        EventUser eventUser = getEventUser(userId);
        EventFrame eventFrame = getEventFrame(eventFrameId);

        // 하루에 여러 번의 기대평을 작성하려 할 때 예외처리
        if(commentRepository.existsByCreatedDateAndEventUser(eventUser.getId())) {
            throw new CommentException(ErrorCode.COMMENT_ALREADY_EXISTS);
        }

        boolean isPositive = commentValidator.analyzeComment(dto.getContent());

        // TODO: 점수정책와 연계하여 기대평 등록 시 점수를 부여 추가해야함
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
    public void deleteComments(List<Long> commentIds) {
        commentRepository.deleteAllById(commentIds);
        log.info("deleted comments: {}", commentIds);
    }

    public ResponseCommentsDto searchComments(String eventId, Integer page, Integer size) {
        PageRequest pageInfo = PageRequest.of(page, size);

        var comments = commentRepository.findAllByEventId(eventId,pageInfo)
                .getContent().stream().map(ResponseCommentDto::from).toList();
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
