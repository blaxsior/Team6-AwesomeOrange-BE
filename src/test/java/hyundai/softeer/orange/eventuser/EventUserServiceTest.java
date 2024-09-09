package hyundai.softeer.orange.eventuser;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.dto.TokenDto;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.core.jwt.JWTManager;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.eventuser.dto.RequestAuthCodeDto;
import hyundai.softeer.orange.eventuser.dto.RequestUserDto;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.exception.EventUserException;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import hyundai.softeer.orange.eventuser.service.EventUserService;
import hyundai.softeer.orange.eventuser.service.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

class EventUserServiceTest {

    @InjectMocks
    private EventUserService eventUserService;

    @Mock
    private SmsService smsService;

    @Mock
    private EventUserRepository eventUserRepository;

    @Mock
    private EventFrameRepository eventFrameRepository;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JWTManager jwtManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        given(eventFrameRepository.findByFrameId(eventFrameId)).willReturn(Optional.of(eventFrame));
        given(stringRedisTemplate.opsForValue()).willReturn(valueOperations);
    }

    RequestUserDto requestUserDto = RequestUserDto.builder()
            .name("test")
            .phoneNumber("01012345678")
            .build();
    TokenDto tokenDto = new TokenDto("token");
    EventFrame eventFrame = EventFrame.of("the-new-ioniq5", "eventFrame");
    EventUser eventUser = EventUser.of("test", "01000000000", eventFrame, "uuid");
    String eventFrameId = "the-new-ioniq5";

    @DisplayName("login: 유저가 로그인한다.")
    @Test
    void loginTest() {
        // given
        given(eventUserRepository.findByUserNameAndPhoneNumberAndEventFrameFrameId(requestUserDto.getName(), requestUserDto.getPhoneNumber(), eventFrameId))
                .willReturn(Optional.of(eventUser));

        // when
        TokenDto result = eventUserService.login(requestUserDto, eventFrameId);

        // then
        assertThat(result).isNotNull();
    }

    @DisplayName("login: 유저가 로그인 시 유저를 찾을 수 없어 예외가 발생한다.")
    @Test
    void loginNotFoundTest() {
        // given
        given(eventUserRepository.findByUserNameAndPhoneNumberAndEventFrameFrameId(requestUserDto.getName(), requestUserDto.getPhoneNumber(), eventFrameId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> eventUserService.login(requestUserDto, eventFrameId))
                .isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.EVENT_USER_NOT_FOUND.getErrorCode());
    }

    @DisplayName("sendAuthCode: 유저가 인증번호를 전송한다.")
    @Test
    void sendAuthCodeTest() {
        // given
        given(eventFrameRepository.existsByFrameId(eventFrameId))
                .willReturn(true);
        given(eventUserRepository.existsByPhoneNumberAndEventFrameFrameId(requestUserDto.getPhoneNumber(), eventFrameId))
                .willReturn(false);
        doNothing().when(smsService).sendSms(requestUserDto);

        // when
        eventUserService.sendAuthCode(requestUserDto, eventFrameId);

        // then
        verify(eventFrameRepository).existsByFrameId(eventFrameId);
        verify(eventUserRepository).existsByPhoneNumberAndEventFrameFrameId(requestUserDto.getPhoneNumber(), eventFrameId);
        verify(smsService).sendSms(requestUserDto);
    }

    @DisplayName("sendAuthCode: 유저가 인증번호를 전송하려 할 때 이벤트 프레임을 찾을 수 없으면 예외가 발생한다.")
    @Test
    void sendAuthCodeNotFoundTest() {
        // given
        given(eventFrameRepository.existsByFrameId(eventFrameId))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> eventUserService.sendAuthCode(requestUserDto, eventFrameId))
                .isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.EVENT_FRAME_NOT_FOUND.getErrorCode());
    }

    @DisplayName("sendAuthCode: 유저가 인증번호를 전송하려 할 때 이미 가입되었음이 확인될 경우 예외가 발생한다.")
    @Test
    void sendAuthCodeConflictTest() {
        // given
        given(eventFrameRepository.existsByFrameId(eventFrameId))
                .willReturn(true);
        given(eventUserRepository.existsByPhoneNumberAndEventFrameFrameId(requestUserDto.getPhoneNumber(), eventFrameId))   // 이미 가입된 유저
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> eventUserService.sendAuthCode(requestUserDto, eventFrameId))
                .isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.EVENTUSER_ALREADY_EXISTS.getErrorCode());
    }

    @DisplayName("checkAuthCode: 유저가 전송한 인증번호를 Redis 상에서 확인하고 성공한다.")
    @ParameterizedTest
    @ValueSource(strings = {"123456", "654321", "111111", "921345"})
    void checkAuthCodeTest(String authCode) {
        // given
        given(stringRedisTemplate.opsForValue().get(eventUser.getPhoneNumber())).willReturn(authCode);
        given(eventFrameRepository.findById(any())).willReturn(Optional.of(eventFrame));
        given(jwtManager.generateToken(anyString(), anyMap(), eq(ConstantUtil.JWT_LIFESPAN)))
                .willReturn(tokenDto.token());
        RequestAuthCodeDto requestAuthCodeDto = RequestAuthCodeDto.builder()
                .name(eventUser.getUserName())
                .phoneNumber(eventUser.getPhoneNumber())
                .authCode(authCode)
                .build();

        // when
        TokenDto result = eventUserService.checkAuthCode(requestAuthCodeDto, eventFrameId);

        // then
        assertThat(result.token()).isEqualTo(tokenDto.token());
    }

    @DisplayName("checkAuthCode: 유저가 전송한 인증번호를 Redis 상에서 확인하고 실패한다.")
    @ParameterizedTest(name = "authCode: {0}, requestAuthCode: {1}")
    @CsvSource({
            "123456, 1234567",
            "654321, 6543210",
            "111111, 1111111",
            "921345, 9213450"
    })
    void checkAuthCodeFailTest(String authCode, String requestAuthCode) {
        // given
        given(stringRedisTemplate.opsForValue().get(requestUserDto.getPhoneNumber())).willReturn(authCode);
        RequestAuthCodeDto requestAuthCodeDto = RequestAuthCodeDto.builder()
                .phoneNumber(requestUserDto.getPhoneNumber())
                .authCode(requestAuthCode)
                .build();

        // when & then
        assertThatThrownBy(() -> eventUserService.checkAuthCode(requestAuthCodeDto, eventFrameId))
                .isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.INVALID_AUTH_CODE.getErrorCode());
    }

    @DisplayName("checkAuthCode: Redis에 저장된 인증번호가 없어 예외가 발생한다.")
    @Test
    void checkAuthCodeBadRequestTest() {
        // given
        given(stringRedisTemplate.opsForValue().get(requestUserDto.getPhoneNumber())).willReturn(null);
        RequestAuthCodeDto requestAuthCodeDto = RequestAuthCodeDto.builder()
                .phoneNumber(requestUserDto.getPhoneNumber())
                .authCode("123456")
                .build();

        // when & then
        assertThatThrownBy(() -> eventUserService.checkAuthCode(requestAuthCodeDto, eventFrameId))
                .isInstanceOf(EventUserException.class)
                .hasMessage(ErrorCode.AUTH_CODE_EXPIRED.getErrorCode());
    }
}
