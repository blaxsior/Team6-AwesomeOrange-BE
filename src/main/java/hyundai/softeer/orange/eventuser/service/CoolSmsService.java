package hyundai.softeer.orange.eventuser.service;

import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.eventuser.config.CoolSmsApiConfig;
import hyundai.softeer.orange.eventuser.dto.RequestUserDto;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.sdk.NurigoApp;
import net.nurigo.sdk.message.model.Message;
import net.nurigo.sdk.message.request.SingleMessageSendingRequest;
import net.nurigo.sdk.message.response.SingleMessageSentResponse;
import net.nurigo.sdk.message.service.DefaultMessageService;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Primary
@Service
public class CoolSmsService implements SmsService {

    private final DefaultMessageService defaultMessageService;
    private final CoolSmsApiConfig coolSmsApiConfig;
    private final StringRedisTemplate stringRedisTemplate;

    public CoolSmsService(CoolSmsApiConfig coolSmsApiConfig, StringRedisTemplate stringRedisTemplate) {
        this.defaultMessageService = NurigoApp.INSTANCE.initialize(coolSmsApiConfig.getApiKey(), coolSmsApiConfig.getApiSecret(), coolSmsApiConfig.getUrl());
        this.coolSmsApiConfig = coolSmsApiConfig;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public void sendSms(RequestUserDto dto) {
        String authCode = generateAuthCode();
        Message message = new Message();
        message.setFrom(coolSmsApiConfig.getFrom());
        message.setTo(dto.getPhoneNumber());
        message.setText("[소프티어 오렌지] 인증번호는 (" + authCode + ")입니다.");

        SingleMessageSentResponse response = defaultMessageService.sendOne(new SingleMessageSendingRequest(message));

        // 5분 동안 인증번호 유효
        stringRedisTemplate.opsForValue().set(dto.getPhoneNumber(), authCode, ConstantUtil.AUTH_CODE_EXPIRE_TIME, TimeUnit.MINUTES);
        log.info("successfully send SMS to {}, response: {}", dto.getPhoneNumber(), response);
    }

    // 6자리 난수 인증번호 생성
    private String generateAuthCode() {
        StringBuilder authCode = new StringBuilder();
        Random random = new Random();
        for(int i=0; i<ConstantUtil.AUTH_CODE_LENGTH; i++){
            authCode.append(random.nextInt(10));
        }
        return authCode.toString();
    }
}
