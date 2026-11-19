package com.ensambla.backend.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityErrorHandlerTest {

    private static final String MOTIVO_INTERNO = "secreto";

    private final SecurityErrorHandler securityErrorHandler = new SecurityErrorHandler(JsonMapper.builder().build());

    @Test
    @DisplayName("Responde 401 JSON sin exponer la excepción interna")
    void responde401JSONSinExponerLaExcepcionInterna() throws Exception {
        MockHttpServletResponse respuesta = cuandoFallaLaAutenticacion();

        entoncesResponde401SinExponerElMotivoInterno(respuesta);
    }

    @Test
    @DisplayName("Responde 403 JSON sin exponer información interna del permiso")
    void responde403JSONSinExponerInformacionInternaDelPermiso() throws Exception {
        MockHttpServletResponse respuesta = cuandoSeDeniegaElAcceso();

        entoncesResponde403SinExponerElMotivoInterno(respuesta);
    }

    // --- cuando ---
    private MockHttpServletResponse cuandoFallaLaAutenticacion() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        securityErrorHandler.commence(new MockHttpServletRequest("GET", "/api/auth/actual"), respuesta,
                new InsufficientAuthenticationException(MOTIVO_INTERNO));
        return respuesta;
    }

    private MockHttpServletResponse cuandoSeDeniegaElAcceso() throws Exception {
        MockHttpServletResponse respuesta = new MockHttpServletResponse();
        securityErrorHandler.handle(new MockHttpServletRequest("POST", "/api/auth/login"), respuesta,
                new AccessDeniedException(MOTIVO_INTERNO));
        return respuesta;
    }

    // --- entonces ---
    private void entoncesResponde401SinExponerElMotivoInterno(MockHttpServletResponse respuesta) throws Exception {
        assertThat(respuesta.getStatus()).isEqualTo(401);
        assertThat(respuesta.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(respuesta.getContentAsString()).contains("Se requiere una sesión válida", "/api/auth/actual")
                .doesNotContain(MOTIVO_INTERNO);
    }

    private void entoncesResponde403SinExponerElMotivoInterno(MockHttpServletResponse respuesta) throws Exception {
        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(respuesta.getContentAsString()).contains("Acceso no permitido").doesNotContain(MOTIVO_INTERNO);
    }
}
