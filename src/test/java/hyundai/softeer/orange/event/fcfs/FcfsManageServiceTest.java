package hyundai.softeer.orange.event.fcfs;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsInfoDto;
import hyundai.softeer.orange.event.fcfs.dto.ResponseFcfsWinnerDto;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEvent;
import hyundai.softeer.orange.event.fcfs.entity.FcfsEventWinningInfo;
import hyundai.softeer.orange.event.fcfs.exception.FcfsEventException;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventRepository;
import hyundai.softeer.orange.event.fcfs.repository.FcfsEventWinningInfoRepository;
import hyundai.softeer.orange.event.fcfs.service.FcfsManageService;
import hyundai.softeer.orange.event.fcfs.util.FcfsUtil;
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
import org.springframework.data.redis.core.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class FcfsManageServiceTest {

    @InjectMocks
    private FcfsManageService fcfsManageService;

    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private FcfsEventRepository fcfsEventRepository;

    @Mock
    private FcfsEventWinningInfoRepository fcfsEventWinningInfoRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    String eventId = "HD_240808_001";
    Long fcfsEventId = 1L;
    EventFrame eventFrame = EventFrame.of("the-new-ioniq5","FcfsManageServiceTest");
    EventUser eventUser = EventUser.of("test", "0101234567", eventFrame, "uuid");
    FcfsEvent fcfsEvent = FcfsEvent.builder()
            .startTime(LocalDateTime.now().plusSeconds(10))
            .participantCount(100L)
            .build();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(eventId)).willReturn(fcfsEventId.toString());
    }

    @DisplayName("registerFcfsEvents: 오늘의 선착순 이벤트 정보(당첨자 수, 시작 시각)를 배치")
    @Test
    void registerFcfsEventsTest() {
        // given
        given(fcfsEventRepository.findByStartTimeBetween(LocalDateTime.now(), LocalDateTime.now().plusDays(1)))
                .willReturn(new ArrayList<>(List.of(fcfsEvent)));

        // when
        fcfsManageService.registerFcfsEvents();

        // then
        verify(fcfsEventRepository).findByStartTimeBetween(any(), any());
        // TODO: Redis ValueOperations에 대한 verify 추가
    }

    @DisplayName("registerWinners: redis에 저장된 모든 선착순 이벤트의 당첨자 정보를 DB로 이관")
    @Test
    void registerWinnersTest() {
        // given
        given(stringRedisTemplate.keys("*:fcfs")).willReturn(Set.of("1:fcfs"));
        given(stringRedisTemplate.opsForZSet()).willReturn(zSetOperations);
        given(zSetOperations.range(FcfsUtil.winnerFormatting(fcfsEventId.toString()), 0, -1))
                .willReturn(Set.of(eventUser.getUserId()));
        given(fcfsEventRepository.findById(fcfsEventId)).willReturn(Optional.of(fcfsEvent));
        given(eventUserRepository.findAllByUserId(List.of(eventUser.getUserId()))).willReturn(List.of(eventUser));

        // when
        fcfsManageService.registerWinners();

        // then
        verify(stringRedisTemplate).keys("*:fcfs");
        verify(zSetOperations).range(FcfsUtil.winnerFormatting(fcfsEventId.toString()), 0, -1);
        verify(fcfsEventRepository).findById(fcfsEventId);
        verify(eventUserRepository).findAllByUserId(List.of(eventUser.getUserId()));
        verify(fcfsEventWinningInfoRepository).saveAll(any());
    }

    @DisplayName("getFcfsInfo: 특정 선착순 이벤트의 정보를 조회하며, 이벤트 시작시간 직후부터 7시간 동안 progress 상태여야 한다.")
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 200, 418, 419})
    void getFcfsInfoProgressTest(int minute) {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(eventId)).willReturn(fcfsEventId.toString());
        given(valueOperations.get(FcfsUtil.startTimeFormatting(fcfsEventId.toString())))
                .willReturn(LocalDateTime.now().minusMinutes(minute).toString());

        // when
        ResponseFcfsInfoDto fcfsInfo = fcfsManageService.getFcfsInfo(eventId);

        // then
        assertThat(fcfsInfo.getEventStatus()).isEqualTo(ConstantUtil.PROGRESS);
    }

    @DisplayName("getFcfsInfo: 특정 선착순 이벤트의 정보를 조회하며, 이벤트 시작시간 3시간 전부터 직전까지는 countdown 상태여야 한다.")
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 120, 178, 179})
    void getFcfsInfoCountdownTest(int minute) {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FcfsUtil.startTimeFormatting(fcfsEventId.toString())))
                .willReturn(LocalDateTime.now().plusMinutes(minute).toString());

        // when
        ResponseFcfsInfoDto fcfsInfo = fcfsManageService.getFcfsInfo(eventId);

        // then
        assertThat(fcfsInfo.getEventStatus()).isEqualTo(ConstantUtil.COUNTDOWN);
    }

    @DisplayName("getFcfsInfo: 특정 선착순 이벤트의 정보를 조회하며, 이벤트 시작시간 7시간 이후부터는 waiting 상태여야 한다.")
    @ParameterizedTest
    @ValueSource(ints = {420, 421, 422})
    void getFcfsInfoWaitingTest(int minute) {
        // given
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FcfsUtil.startTimeFormatting(fcfsEventId.toString())))
                .willReturn(LocalDateTime.now().minusMinutes(minute).toString());

        // when
        ResponseFcfsInfoDto fcfsInfo = fcfsManageService.getFcfsInfo(eventId);

        // then
        assertThat(fcfsInfo.getEventStatus()).isEqualTo(ConstantUtil.WAITING);
    }

    @DisplayName("getFcfsInfo: 특정 선착순 이벤트의 시간을 찾을 수 없는 경우 예외가 발생한다.")
    @Test
    void getFcfsInfoNotFoundTest() {
        // when
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(FcfsUtil.startTimeFormatting(fcfsEventId.toString())))
                .willReturn(null);

        assertThatThrownBy(() -> fcfsManageService.getFcfsInfo(eventId))
                .isInstanceOf(FcfsEventException.class)
                .hasMessage(ErrorCode.FCFS_EVENT_NOT_FOUND.getMessage());
    }

    @DisplayName("isParticipated: 특정 선착순 이벤트에 참여한 유저임을 확인한다.")
    @Test
    void isParticipatedTest() {
        // given
        given(fcfsEventRepository.existsById(fcfsEventId)).willReturn(true);
        given(stringRedisTemplate.opsForSet()).willReturn(setOperations);
        given(setOperations.isMember(FcfsUtil.participantFormatting(fcfsEventId.toString()), eventUser.getUserId())).willReturn(true);

        // when
        boolean participated = fcfsManageService.isParticipated(eventId, eventUser.getUserId());

        // then
        assertThat(participated).isTrue();
    }

    @DisplayName("isParticipated: 선착순 이벤트가 존재하지 않는 경우 예외가 발생한다.")
    @Test
    void isParticipatedNotFoundTest() {
        // given
        given(fcfsEventRepository.existsById(fcfsEventId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> fcfsManageService.isParticipated(eventId, eventUser.getUserId()))
                .isInstanceOf(FcfsEventException.class)
                .hasMessage(ErrorCode.FCFS_EVENT_NOT_FOUND.getMessage());
    }

    @DisplayName("getFcfsWinnersInfo: 특정 선착순 이벤트의 당첨자 조회 - 어드민에서 사용")
    @Test
    void getFcfsWinnersInfoTest() {
        // given
        LocalDateTime now = LocalDateTime.now();
        given(fcfsEventWinningInfoRepository.findByFcfsEventId(fcfsEventId))
                .willReturn(List.of(FcfsEventWinningInfo.of(fcfsEvent, eventUser, now)));

        // when
        List<ResponseFcfsWinnerDto> fcfsWinnersInfo = fcfsManageService.getFcfsWinnersInfo(fcfsEventId);

        // then
        assertThat(fcfsWinnersInfo).hasSize(1);
        assertThat(fcfsWinnersInfo.get(0).getName()).isEqualTo(eventUser.getUserName());
        assertThat(fcfsWinnersInfo.get(0).getPhoneNumber()).isEqualTo(eventUser.getPhoneNumber());
        assertThat(fcfsWinnersInfo.get(0).getWinningTime()).isEqualTo(now);
    }
}
