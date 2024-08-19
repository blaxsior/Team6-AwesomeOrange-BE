package hyundai.softeer.orange.event.fcfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.ErrorResponse;
import hyundai.softeer.orange.core.auth.AuthInterceptor;
import hyundai.softeer.orange.event.fcfs.controller.FcfsController;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsInfoDto;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsResultDto;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.service.FcfsAnswerService;
import hyundai.softeer.orange.event.fcfs.service.FcfsManageService;
import hyundai.softeer.orange.event.fcfs.service.FcfsService;
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

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = FcfsController.class)
class FcfsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private FcfsService fcfsService;

    @MockBean
    private FcfsAnswerService fcfsAnswerService;

    @MockBean
    private FcfsManageService fcfsManageService;

    @MockBean
    private EventUserArgumentResolver eventUserArgumentResolver;

    @MockBean
    private AuthInterceptor authInterceptor;

    String userId = "testUserId";
    String answer = "answer";
    String eventId = "HD_240808_001";

    @BeforeEach
    void setUp() throws Exception {
        EventUserInfo mockUserInfo = new EventUserInfo(userId, "event_user");
        when(authInterceptor.preHandle(any(), any(), any())).thenReturn(true);
        when(eventUserArgumentResolver.supportsParameter(any())).thenReturn(true);
        when(eventUserArgumentResolver.resolveArgument(any(), any(), any(), any()))
                .thenReturn(mockUserInfo);
    }

    @DisplayName("participate: 정답을 맞힌 상태에서 선착순 이벤트 참여 혹은 실패")
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void participateTest(boolean isWinner) throws Exception {
        // given
        ResponseFcfsResultDto responseFcfsResultDto = new ResponseFcfsResultDto(true, isWinner);
        when(fcfsAnswerService.judgeAnswer(eventId, answer)).thenReturn(true);
        when(fcfsService.participate(eventId, userId)).thenReturn(isWinner);
        String requestBody = mapper.writeValueAsString(answer);
        String responseBody = mapper.writeValueAsString(responseFcfsResultDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/event/fcfs/" + eventId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
        verify(fcfsService, times(1)).participate(eventId, userId);
    }

    @DisplayName("participate: 정답을 맞히지 못하면 무조건 참여 실패하며 fcfsService에 접근조차 하지 않는다.")
    @Test
    void participateWrongAnswerTest() throws Exception {
        // given
        ResponseFcfsResultDto responseFcfsResultDto = new ResponseFcfsResultDto(false, false);
        when(fcfsAnswerService.judgeAnswer(eventId, answer)).thenReturn(false);
        String requestBody = mapper.writeValueAsString(answer);
        String responseBody = mapper.writeValueAsString(responseFcfsResultDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/event/fcfs/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
        verify(fcfsService, never()).participate(eventId, userId);
    }

    @DisplayName("participate: 선착순 이벤트 참여 시 이벤트 시간이 아니어서 예외가 발생하는 경우")
    @Test
    void participate400Test() throws Exception {
        // given
        when(fcfsAnswerService.judgeAnswer(eventId, answer)).thenReturn(true);
        when(fcfsService.participate(eventId, userId)).thenThrow(new FcfsEventException(ErrorCode.INVALID_EVENT_TIME));
        String requestBody = mapper.writeValueAsString(answer);
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.INVALID_EVENT_TIME));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/event/fcfs/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("participate: 선착순 이벤트 참여 시 eventId가 잘못된 경우")
    @ParameterizedTest(name = "eventId: {0}")
    @ValueSource(strings = {"a", "1.1", "1.0", "1.1.1"})
    void participateBadInputTest(String eventId) throws Exception {
        // given
        when(fcfsAnswerService.judgeAnswer(eventId, answer)).thenReturn(true);
        when(fcfsService.participate(eventId, userId)).thenThrow(new FcfsEventException(ErrorCode.FCFS_EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/event/fcfs/" + eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(answer)))
                .andExpect(status().isNotFound());
    }

    @DisplayName("getFcfsInfo: 선착순 이벤트에 대한 정보(서버 기준 시각, 이벤트의 상태)를 조회한다.")
    @Test
    void getFcfsInfoTest() throws Exception {
        // given
        ResponseFcfsInfoDto responseFcfsInfoDto = new ResponseFcfsInfoDto(LocalDateTime.now(), "waiting");
        when(fcfsManageService.getFcfsInfo(eventId)).thenReturn(responseFcfsInfoDto);
        String responseBody = mapper.writeValueAsString(responseFcfsInfoDto);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/event/fcfs/{eventId}/info", eventId))
                .andExpect(status().isOk())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("getFcfsInfo: 선착순 이벤트를 찾을 수 없는 경우")
    @Test
    void getFcfsInfo404Test() throws Exception {
        // given
        when(fcfsManageService.getFcfsInfo(eventId)).thenThrow(new FcfsEventException(ErrorCode.EVENT_NOT_FOUND));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/event/fcfs/{eventId}/info", eventId))
                .andExpect(status().isNotFound())
                .andExpect(content().json(responseBody));
    }

    @DisplayName("isParticipated: 유저의 특정 선착순 이벤트 참여 여부를 조회한다.")
    @Test
    void isParticipatedTest() throws Exception {
        // given
        when(fcfsManageService.isParticipated(eventId, userId)).thenReturn(true);

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/event/fcfs/" + eventId + "/participated"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @DisplayName("isParticipated: 선착순 이벤트를 찾을 수 없는 경우")
    @Test
    void isParticipated404Test() throws Exception {
        // given
        when(fcfsManageService.isParticipated(eventId, userId)).thenThrow(new FcfsEventException(ErrorCode.EVENT_NOT_FOUND));
        String responseBody = mapper.writeValueAsString(ErrorResponse.from(ErrorCode.EVENT_NOT_FOUND));

        // when & then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/event/fcfs/" + eventId + "/participated"))
                .andExpect(status().isNotFound())
                .andExpect(content().json(responseBody));
    }
}
