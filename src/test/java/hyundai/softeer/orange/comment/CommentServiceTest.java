package hyundai.softeer.orange.comment;

import hyundai.softeer.orange.comment.dto.CreateCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentsDto;
import hyundai.softeer.orange.comment.entity.Comment;
import hyundai.softeer.orange.comment.exception.CommentException;
import hyundai.softeer.orange.comment.repository.CommentRepository;
import hyundai.softeer.orange.comment.service.CommentService;
import hyundai.softeer.orange.comment.service.CommentValidator;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentValidator commentValidator;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private EventFrameRepository eventFrameRepository;

    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private EventFrame eventFrame;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.of(eventFrame));
        given(eventFrame.getId()).willReturn(1L);
    }

    Long commentId = 1L;
    String eventFrameId = "the-new-ioniq5";
    CreateCommentDto createCommentDto = new CreateCommentDto("test");
    EventUser eventUser = EventUser.of("test", "01012345678", null, "uuid");
    Pageable pageable = PageRequest.of(0, ConstantUtil.COMMENTS_SIZE);

    @DisplayName("getComments: 무작위 긍정 기대평 목록을 조회한다.")
    @Test
    void getCommentsTest() {
        // given
        ResponseCommentDto responseCommentDto = ResponseCommentDto.builder()
                .id(1L)
                .content("test")
                .userName("test")
                .createdAt(LocalDateTime.now())
                .build();
        given(commentRepository.findRandomPositiveComments(anyLong(), eq(pageable)))
                .willReturn(List.of(responseCommentDto));

        // when
        ResponseCommentsDto dto = commentService.getComments(eventFrameId);

        // then
        assertThat(dto.getComments()).hasSize(1);
        assertThat(dto.getComments().get(0).getContent()).isEqualTo("test");
        verify(commentRepository, times(1)).findRandomPositiveComments(anyLong(), any(Pageable.class));
    }

    @DisplayName("getComments: 무작위 긍정 기대평 목록이 없는 경우 빈 목록을 반환한다.")
    @Test
    void getCommentsTestEmpty() {
        // given
        given(commentRepository.findRandomPositiveComments(anyLong(), eq(pageable))).willReturn(List.of());

        // when
        ResponseCommentsDto dto = commentService.getComments(eventFrameId);

        // then
        assertThat(dto.getComments()).isEmpty();
        verify(commentRepository, times(1)).findRandomPositiveComments(anyLong(), any(Pageable.class));
    }

    @DisplayName("createComment: 신규 기대평을 작성한다.")
    @Test
    void createCommentTest() {
        // given
        given(commentRepository.existsByCreatedDateAndEventUser(any())).willReturn(false);
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.of(eventFrame));
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.ofNullable(eventUser));
        given(commentValidator.analyzeComment(createCommentDto.getContent())).willReturn(true);
        given(commentRepository.save(any())).willReturn(Comment.of("test", eventFrame, eventUser, true));

        // when
        commentService.createComment(eventUser.getUserId(), eventFrameId, createCommentDto);

        // then
        verify(commentRepository, times(1)).save(any());
        verify(commentRepository, times(1)).existsByCreatedDateAndEventUser(any());
        verify(eventFrameRepository, times(1)).findByFrameId(eventFrameId);
        verify(eventUserRepository, times(1)).findByUserId(eventUser.getUserId());
        verify(commentRepository, times(1)).save(any());
    }

    @DisplayName("createComment: 하루에 여러 번의 기대평을 작성하려 할 때 예외가 발생한다.")
    @Test
    void createCommentAlreadyExistsTest() {
        // given
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.of(eventFrame));
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.of(eventUser));
        given(commentRepository.existsByCreatedDateAndEventUser(any())).willReturn(true);

        // when
        assertThatThrownBy(() -> commentService.createComment(eventUser.getUserId(), eventFrameId, createCommentDto))
                .isInstanceOf(CommentException.class)
                .hasMessage(ErrorCode.COMMENT_ALREADY_EXISTS.getMessage());
    }

    @DisplayName("createComment: EventFrame을 찾을 수 없는 경우 예외가 발생한다.")
    @Test
    void createCommentFrameNotFoundTest() {
        // given
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.ofNullable(eventUser));
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(eventUser.getUserId(), eventFrameId, createCommentDto))
                .isInstanceOf(CommentException.class)
                .hasMessage(ErrorCode.EVENT_FRAME_NOT_FOUND.getMessage());
    }

    @DisplayName("createComment: EventUser을 찾을 수 없는 경우 예외가 발생한다.")
    @Test
    void createCommentUserNotFoundTest() {
        // given
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> commentService.createComment(eventUser.getUserId(), eventFrameId, createCommentDto))
                .isInstanceOf(CommentException.class)
                .hasMessage(ErrorCode.EVENT_USER_NOT_FOUND.getMessage());
    }

    @DisplayName("createComment: 기대평이 부정적인 경우 예외가 발생한다.")
    @Test
    void createCommentNegativeTest() {
        // given
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.of(eventFrame));
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.ofNullable(eventUser));
        given(commentValidator.analyzeComment(createCommentDto.getContent())).willThrow(new CommentException(ErrorCode.INVALID_COMMENT));

        // when & then
        assertThatThrownBy(() -> commentService.createComment(eventUser.getUserId(), eventFrameId, createCommentDto))
                .isInstanceOf(CommentException.class)
                .hasMessage(ErrorCode.INVALID_COMMENT.getMessage());
    }

    @DisplayName("isCommentable: 오늘 이 유저가 기대평을 작성할 수 있는지 조회한다.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isCommentableTest(boolean exists) {
        // given
        given(eventUserRepository.findByUserId(eventUser.getUserId())).willReturn(Optional.ofNullable(eventUser));
        given(commentRepository.existsByCreatedDateAndEventUser(eventUser.getId())).willReturn(exists);

        // when
        Boolean isCommentable = commentService.isCommentable(eventUser.getUserId());

        // then
        assertThat(isCommentable).isEqualTo(!exists);
        verify(commentRepository, times(1)).existsByCreatedDateAndEventUser(eventUser.getId());
    }

    @DisplayName("deleteComment: commentId로 기대평을 찾아 삭제한다.")
    @Test
    void deleteCommentTest() {
        // given
        given(commentRepository.existsById(commentId)).willReturn(true);
        doNothing().when(commentRepository).deleteById(commentId);

        // when
        Long deletedCommentId = commentService.deleteComment(commentId);

        // then
        assertThat(deletedCommentId).isEqualTo(commentId);
        verify(commentRepository, times(1)).deleteById(commentId);
    }

    @DisplayName("deleteComment: 기대평을 찾을 수 없는 경우 예외가 발생한다.")
    @Test
    void deleteCommentNotFoundTest() {
        // given
        given(commentRepository.existsById(commentId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> commentService.deleteComment(commentId))
                .isInstanceOf(CommentException.class)
                .hasMessage(ErrorCode.COMMENT_NOT_FOUND.getMessage());
    }

    @DisplayName("searchComments: eventId로 기대평 목록을 조회한다.")
    @Test
    void searchCommentsTest() {
        // given
        given(commentRepository.findAllByEventId(any(), any())).willReturn(new PageImpl<>(List.of(Comment.of("test", eventFrame, eventUser, true))));

        // when
        ResponseCommentsDto dto = commentService.searchComments(eventFrameId, null,0, 10);

        // then
        assertThat(dto.getComments()).hasSize(1);
        assertThat(dto.getComments().get(0).getContent()).isEqualTo("test");
        verify(commentRepository, times(1)).findAllByEventId(any(), any());
    }
}
