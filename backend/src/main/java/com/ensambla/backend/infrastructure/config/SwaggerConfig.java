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
        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("csrf", claveEn(SecurityScheme.In.HEADER, "X-CSRF-TOKEN"))
                        .addSecuritySchemes("sesion", claveEn(SecurityScheme.In.COOKIE, nombreCookie)))
                .info(new Info()
                        .title("Ensambla API")
                        .description("Documentación REST del backend de Ensambla")
                        .version("0.0.1"));
    }

    private static SecurityScheme claveEn(SecurityScheme.In lugar, String nombre) {
        return new SecurityScheme().type(SecurityScheme.Type.APIKEY).in(lugar).name(nombre);
    }
}
