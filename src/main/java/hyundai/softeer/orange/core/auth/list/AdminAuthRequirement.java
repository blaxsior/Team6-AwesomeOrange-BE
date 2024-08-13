package hyundai.softeer.orange.core.auth.list;

import hyundai.softeer.orange.core.auth.AuthConst;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SecurityRequirement(name = AuthConst.ADMIN_AUTH)
public @interface AdminAuthRequirement {
}
