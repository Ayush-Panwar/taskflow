package com.taskflow.common

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Swagger UI is at /swagger-ui.html. Log in, then paste the accessToken into "Authorize". */
@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("TaskFlow API")
                .version("1.0")
                .description("A small team task tracker. Call POST /api/auth/login, then use Authorize with the accessToken."),
        )
        .components(
            Components().addSecuritySchemes(
                BEARER_AUTH,
                SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"),
            ),
        )
        .addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))

    private companion object {
        const val BEARER_AUTH = "bearerAuth"
    }
}
