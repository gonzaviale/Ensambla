package com.ensambla.backend.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${server.servlet.session.cookie.name:ENSAMBLA_SESSION}")
    private String nombreCookie = "ENSAMBLA_SESSION";

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().components(new Components()
                .addSecuritySchemes("csrf", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-CSRF-TOKEN"))
                .addSecuritySchemes("sesion", new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name(nombreCookie)))
                .info(new Info()
                .title("Ensambla API")
                .description("Documentación REST del backend de Ensambla")
                .version("0.0.1"));
    }
}
