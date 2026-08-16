package com.ofood.security.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "OFOOD Authentication API",
        version = "1.0",
        description = "API documentation for OFOOD Authentication and Authorization endpoints."
    )
)
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT",
    description = "JWT Authorization header using the Bearer scheme. Used for authenticated endpoints like /logout and /change-password. Note: The API enforces RS256 algorithm. The token payload includes roles and a session ID (sid)."
)
public class OpenApiConfig {
}
