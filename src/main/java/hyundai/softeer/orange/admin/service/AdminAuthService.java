package hyundai.softeer.orange.admin.service;

import hyundai.softeer.orange.admin.dto.AdminDto;
import hyundai.softeer.orange.admin.entity.Admin;
import hyundai.softeer.orange.admin.exception.AdminException;
import hyundai.softeer.orange.admin.repository.AdminRepository;
import hyundai.softeer.orange.common.ErrorCode;
import hyundai.softeer.orange.common.util.ConstantUtil;
import hyundai.softeer.orange.core.auth.AuthRole;
import hyundai.softeer.orange.core.jwt.JWTConst;
import hyundai.softeer.orange.core.jwt.JWTManager;
import hyundai.softeer.orange.core.security.PasswordManager;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private static final Logger log = LoggerFactory.getLogger(AdminAuthService.class);
    private final AdminRepository adminRepository;
    private final PasswordManager pwManager;
    private final JWTManager jwtManager;

    /**
     * 관리자 유저가 로그인하는 메서드. 로그인 성공 시 JWT 토큰(문자열) 을 반환한다.
     * @param username 관리자 ID
     * @param password 관리자 비밀번호
     * @return 관리자 유저의 JWT 토큰
     */
    public String signIn(String username, String password) {
        Optional<Admin> adminOptional = adminRepository.findFirstByUserName(username);
        Admin admin = adminOptional.orElseThrow(() -> new AdminException(ErrorCode.AUTHENTICATION_FAILED));
        AdminDto dto = AdminDto.of(admin.getId(), admin.getNickName());
        String beforePassword = admin.getPassword();
        boolean loginSuccess = pwManager.verify(password, beforePassword);
        if(!loginSuccess) throw new AdminException(ErrorCode.AUTHENTICATION_FAILED);

        return jwtManager.generateToken(ConstantUtil.CLAIMS_ADMIN, Map.of(ConstantUtil.CLAIMS_ADMIN, dto, JWTConst.ROLE, AuthRole.admin), 5);
    }

    /**
     * 관리자 유저를 생성하는 메서드.
     * @param userName 관리자 ID
     * @param password 관리자 비밀번호
     * @param nickname 관리자의 닉네임
     */
    public void signUp(String userName, String password, String nickname) {
        boolean exists = adminRepository.existsByUserName(userName);
        if(exists) throw new AdminException(ErrorCode.ADMIN_USER_ALREADY_EXISTS);

        String encryptedPassword = pwManager.encrypt(password);

        Admin admin = Admin.builder()
                .userName(userName)
                .password(encryptedPassword)
                .nickName(nickname)
                .build();
        log.info("admin signed up: {}", admin.getUserName());
        adminRepository.save(admin);
    }
}
