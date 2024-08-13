package hyundai.softeer.orange.comment;

import com.fasterxml.jackson.databind.ObjectMapper;
import hyundai.softeer.orange.comment.controller.CommentController;
import hyundai.softeer.orange.comment.dto.CreateCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentDto;
import hyundai.softeer.orange.comment.dto.ResponseCommentsDto;
import hyundai.softeer.orange.comment.exception.CommentException;
import hyundai.softeer.orange.comment.service.CommentValidator;
import hyundai.softeer.orange.comment.service.CommentService;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.common.util.MessageUtil;
import hyundai.softeer.orange.core.auth.AuthInterceptor;
import hyundai.softeer.orange.eventuser.component.EventUserArgumentResolver;
import hyundai.softeer.orange.eventuser.dto.EventUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private CommentService commentService;

    @MockBean
    private CommentValidator commentValidator;

    @MockBean
    private EventUserArgumentResolver eventUserArgumentResolver;

    @MockBean
    private AuthInterceptor authInterceptor;

    ObjectMapper mapper = new ObjectMapper();
    String eventFrameId = "the-new-ioniq5";
    CreateCommentDto createCommentDto = new CreateCommentDto("hello");
    String requestBody = "";

    @BeforeEach
    void setUp() throws Exception {
        EventUserInfo eventUserInfo = new EventUserInfo("testUserId", "eventUser");
        requestBody = mapper.writeValueAsString(createCommentDto);
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(eventUserArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(eventUserArgumentResolver.resolveArgument(any(), any(), any(), any())).thenReturn(eventUserInfo);
    }

    @DisplayName("getComments: 기대평 조회 API를 호출한다.")
    @Test
    void getComments200Test() throws Exception {
        // given
        List<ResponseCommentDto> comments = List.of(
                ResponseCommentDto.builder().content("기대평1").build(),
                ResponseCommentDto.builder().content("기대평2").build()
        );
        ResponseCommentsDto responseCommentsDto = new ResponseCommentsDto(comments);
        when(commentService.getComments(eventFrameId)).thenReturn(responseCommentsDto);
        String responseBody = mapper.writeValueAsString(responseCommentsDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/comment/" + eventFrameId))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("getComments: 기대평 조회 API를 호출하며, 빈 목록을 반환한다.")
    @Test
    void getComments200EmptyTest() throws Exception {
        // given
        ResponseCommentsDto responseCommentsDto = new ResponseCommentsDto(List.of());
        when(commentService.getComments(eventFrameId)).thenReturn(responseCommentsDto);
        String responseBody = mapper.writeValueAsString(responseCommentsDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/comment/" + eventFrameId))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("createComment: 기대평 등록 API를 호출한다.")
    @Test
    void createComment200Test() throws Exception {
        // given
        when(commentValidator.analyzeComment(createCommentDto.getContent())).thenReturn(true);
        when(commentService.createComment(any(), anyString(), any(CreateCommentDto.class))).thenReturn(true);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/comment/" + eventFrameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true")); // Expect true
    }

    @DisplayName("createComment: 기대평 등록 API를 호출 시 기대평이 지나치게 부정적인 표현으로 간주되어 실패한다.")
    @Test
    void createComment400Test() throws Exception {
        // given
        when(commentValidator.analyzeComment(createCommentDto.getContent())).thenReturn(true);
        when(commentService.createComment(any(), anyString(), any(CreateCommentDto.class)))
                .thenThrow(new CommentException(ErrorCode.INVALID_COMMENT));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.INVALID_COMMENT));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/comment/" + eventFrameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("createComment: 기대평 등록 API를 호출 시 CreateCommentDto의 유효성 검사가 실패한다.")
    @Test
    void createComment400BadInputTest() throws Exception {
        // given
        CreateCommentDto badInput = new CreateCommentDto( "");
        requestBody = mapper.writeValueAsString(badInput);

        Map<String, String> expectedErrors = new HashMap<>();
        expectedErrors.put("content", MessageUtil.OUT_OF_SIZE);
        String responseBody = mapper.writeValueAsString(expectedErrors);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/comment/" + eventFrameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("createComment: 기대평 등록 API를 호출 시 해당 정보를 갖는 유저나 이벤트가 존재하지 않아 실패한다.")
    @Test
    void createComment404Test() throws Exception {
        // given
        when(commentValidator.analyzeComment(createCommentDto.getContent())).thenReturn(true);
        when(commentService.createComment(any(), anyString(), any(CreateCommentDto.class)))
                .thenThrow(new CommentException(ErrorCode.EVENT_USER_NOT_FOUND));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.EVENT_USER_NOT_FOUND));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/comment/" + eventFrameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("createComment: 기대평 등록 API를 호출 시 기대평 중복 작성으로 인해 실패한다.")
    @Test
    void createComment409Test() throws Exception {
        // given
        when(commentValidator.analyzeComment(createCommentDto.getContent())).thenReturn(true);
        when(commentService.createComment(any(), anyString(), any(CreateCommentDto.class)))
                .thenThrow(new CommentException(ErrorCode.COMMENT_ALREADY_EXISTS));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.COMMENT_ALREADY_EXISTS));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .post("/api/v1/comment/" + eventFrameId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("isCommentable: 기대평 등록 가능 여부를 조회한다.")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void isCommentable200Test(boolean isCommentable) throws Exception {
        // given
        when(commentService.isCommentable(any())).thenReturn(isCommentable);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/comment/info"))
                .andExpect(status().isOk())
                .andExpect(content().string(isCommentable ? "true" : "false"));
    }

    @DisplayName("isCommentable: 기대평 등록 가능 여부 조회 시 해당 정보를 갖는 유저가 존재하지 않아 실패한다.")
    @Test
    void isCommentable404Test() throws Exception {
        // given
        when(commentService.isCommentable(any()))
                .thenThrow(new CommentException(ErrorCode.EVENT_USER_NOT_FOUND));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.EVENT_USER_NOT_FOUND));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/v1/comment/info"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(responseBody));
    }
}
