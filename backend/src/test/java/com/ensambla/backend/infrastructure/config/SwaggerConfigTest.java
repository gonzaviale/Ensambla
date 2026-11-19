package com.ensambla.backend.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    private OpenAPI documentacion;

    @Test
    @DisplayName("La documentación identifica el backend de Ensambla con título, versión y descripción")
    void laDocumentacionIdentificaAlBackendDeEnsambla() {
        cuandoSeArmaLaDocumentacion();

        entoncesSeIdentificaComo("Ensambla API", "0.0.1");
    }

    @Test
    @DisplayName("Swagger documenta el header CSRF")
    void swaggerDocumentaElHeaderCsrf() {
        cuandoSeArmaLaDocumentacion();

        entoncesDocumentaLaClave("csrf", SecurityScheme.In.HEADER, "X-CSRF-TOKEN");
    }

    @Test
    @DisplayName("Swagger documenta la cookie de sesión")
    void swaggerDocumentaLaCookieDeSesion() {
        cuandoSeArmaLaDocumentacion();

        entoncesDocumentaLaClave("sesion", SecurityScheme.In.COOKIE, "ENSAMBLA_SESSION");
    }

    private void cuandoSeArmaLaDocumentacion() {
        documentacion = new SwaggerConfig().openAPI();
    }

    private void entoncesSeIdentificaComo(String titulo, String version) {
        assertThat(documentacion.getInfo().getTitle()).isEqualTo(titulo);
        assertThat(documentacion.getInfo().getVersion()).isEqualTo(version);
        assertThat(documentacion.getInfo().getDescription()).contains("Ensambla");
    }

    private void entoncesDocumentaLaClave(String esquema, SecurityScheme.In lugar, String nombre) {
        SecurityScheme clave = documentacion.getComponents().getSecuritySchemes().get(esquema);
        assertThat(clave.getType()).isEqualTo(SecurityScheme.Type.APIKEY);
        assertThat(clave.getIn()).isEqualTo(lugar);
        assertThat(clave.getName()).isEqualTo(nombre);
    }
}