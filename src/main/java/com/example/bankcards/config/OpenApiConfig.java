package com.example.bankcards.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        String name = "Authorization";
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList(name))
                .components(new Components()
                        .addSecuritySchemes(name, new SecurityScheme()
                                .name(name)
                                .type(SecurityScheme.Type.HTTP)
                                .in(SecurityScheme.In.HEADER)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                        )
                )
                .info(new Info().title("Bank REST API").version("1.0"));
    }
}
