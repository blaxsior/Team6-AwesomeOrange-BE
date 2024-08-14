package hyundai.softeer.orange.config;

import hyundai.softeer.orange.core.auth.AuthConst;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(
                        new Components()
                                // 일반화 할 수 있는 방법?
                        .addSecuritySchemes(AuthConst.ADMIN_AUTH, createAPIKeyScheme())
                        .addSecuritySchemes(AuthConst.EVENT_USER_AUTH, createAPIKeyScheme())
                )
//                 모든 경로에 등록하면 안됨.
//                .addSecurityItem(createAPIKeyRequirement())
                .info(apiInfo());
    }

    private Info apiInfo() {
        return new Info()
                .title("Orange BE")
                .description("Awesome Orange Back-End REST API")
                .version("1.0.0");
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme().type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer");
    }

    // 어디에 security가 필요한지 등록하는 것. 기본적으로 모든 경로에 등록됨
    private SecurityRequirement createAPIKeyRequirement() {
        return new SecurityRequirement().addList("Bearer Auth");
    }
}
