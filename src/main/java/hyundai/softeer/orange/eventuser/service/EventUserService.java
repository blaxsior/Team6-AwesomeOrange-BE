package hyundai.softeer.orange.eventuser.service;

import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.dto.TokenDto;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.jwt.JWTConst;
import hyundai.softeer.orange.core.jwt.JWTManager;
import hyundai.softeer.orange.event.common.entity.EventFrame;
import hyundai.softeer.orange.event.common.repository.EventFrameRepository;
import hyundai.softeer.orange.eventuser.dto.EventUserPageDto;
import hyundai.softeer.orange.eventuser.dto.RequestAuthCodeDto;
import hyundai.softeer.orange.eventuser.dto.RequestUserDto;
import hyundai.softeer.orange.eventuser.entity.EventUser;
import hyundai.softeer.orange.eventuser.exception.EventUserException;
import hyundai.softeer.orange.eventuser.repository.EventUserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class EventUserService {

    private static final Logger log = LoggerFactory.getLogger(EventUserService.class);
    private final SmsService smsService;
    private final EventUserRepository eventUserRepository;
    private final EventFrameRepository eventFrameRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final JWTManager jwtManager;

    /**
     * 1. dto 데이터로 DB 조회하여 이미 존재하면 로그인 처리
     * 2. 존재하지 않으면 404 예외
     * 3. 유저 uuid 기반 JWT 토큰 발급
     */
    @Transactional(readOnly = true)
    public TokenDto login(RequestUserDto dto) {
        EventUser eventUser = eventUserRepository.findByUserNameAndPhoneNumber(dto.getName(), dto.getPhoneNumber())
                .orElseThrow(() -> new EventUserException(ErrorCode.EVENT_USER_NOT_FOUND));

        log.info("EventUser {} found, Login success", eventUser.getUserName());
        return generateToken(eventUser);
    }

    @Transactional(readOnly = true)
    public EventUserPageDto getUserBySearch(String search, String field, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<EventUser> userPage = eventUserRepository.findBySearch(search, field, pageRequest);

        return EventUserPageDto.from(userPage);
    }

    /**
     * 1. 유저의 전화번호 및 이벤트 프레임 아이디로 이미 해당 이벤트에 가입된 유저인지 검증
     * 2. 없다면 인증번호 발송
     * @param dto
     * @param eventFrameId
     */
    @Transactional(readOnly = true)
    public void sendAuthCode(RequestUserDto dto, String eventFrameId){
        // 이벤트 프레임이 존재하지 않는 경우
        if(!eventFrameRepository.existsByFrameId(eventFrameId)){
            throw new EventUserException(ErrorCode.EVENT_FRAME_NOT_FOUND);
        }

        // 이미 해당 이벤트에 가입된 유저인 경우
        if(eventUserRepository.existsByPhoneNumberAndEventFrameFrameId(dto.getPhoneNumber(), eventFrameId)){
            throw new EventUserException(ErrorCode.USER_ALREADY_EXISTS);
        }
        smsService.sendSms(dto);
    }

    /**
     * 1. 유저가 입력한 인증번호와 Redis에 저장된 인증번호 비교
     * 2. 일치하면 신규 유저 저장하고 JWT 토큰 발급
     * 3. 불일치하면 401 예외
     * 4. 전화번호로 발송된 인증번호가 존재하지 않는다면 400 예외
     */
    @Transactional
    public TokenDto checkAuthCode(RequestAuthCodeDto dto, String eventFrameId) {
        // Redis에서 인증번호 조회
        String authCode = stringRedisTemplate.opsForValue().get(dto.getPhoneNumber());

        // 해당 전화번호로 발송된 인증번호가 없거나 만료된 경우
        if(authCode == null) {
            throw new EventUserException(ErrorCode.AUTH_CODE_EXPIRED);
        }

        // 인증번호가 틀린 경우
        if (!authCode.equals(dto.getAuthCode())) {
            throw new EventUserException(ErrorCode.INVALID_AUTH_CODE);
        }

        // Redis에 저장된 인증번호 삭제
        stringRedisTemplate.delete(dto.getPhoneNumber());

        // DB에 유저 데이터 저장
        EventFrame eventFrame = eventFrameRepository.findByFrameId(eventFrameId)
                .orElseThrow(() -> new EventUserException(ErrorCode.EVENT_FRAME_NOT_FOUND));
        String userId = UUID.randomUUID().toString().substring(0, ConstantUtil.USER_ID_LENGTH);
        EventUser eventUser = EventUser.of(dto.getName(), dto.getPhoneNumber(), eventFrame, userId);
        eventUserRepository.save(eventUser);
        log.info("EventUser {} saved, Authentication success", eventUser.getUserName());
        return generateToken(eventUser);
    }

    // JWT 토큰 생성
    private TokenDto generateToken(EventUser eventUser) {
        Map<String, Object> claims = Map.of(ConstantUtil.CLAIMS_USER_KEY, eventUser.getUserId(), JWTConst.ROLE, AuthRole.event_user,
                ConstantUtil.CLAIMS_USER_NAME_KEY, eventUser.getUserName());
        String token = jwtManager.generateToken(ConstantUtil.JWT_USER_KEY, claims, ConstantUtil.JWT_LIFESPAN);
        log.info("JWT Token generated for EventUser {}", eventUser.getUserName());
        return new TokenDto(token);
    }
}
