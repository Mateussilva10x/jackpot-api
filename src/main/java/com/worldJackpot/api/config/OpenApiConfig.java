package com.worldJackpot.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "World Jackpot API",
                version = "1.0",
                description = "Documentation for World Jackpot API endpoints. Uses JWT Authentication.",
                contact = @Contact(
                        name = "World Jackpot Team"
                )
        )
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT authentication. Insert the Bearer Token here.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}
