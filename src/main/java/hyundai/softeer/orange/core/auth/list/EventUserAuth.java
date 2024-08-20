package hyundai.softeer.orange.core.auth.list;

import hyundai.softeer.orange.core.auth.Auth;
import hyundai.softeer.orange.core.auth.AuthConst;
import hyundai.softeer.orange.core.auth.AuthRole;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirement(name = AuthConst.EVENT_USER_AUTH)
@Auth({AuthRole.event_user})
public @interface EventUserAuth {
}
