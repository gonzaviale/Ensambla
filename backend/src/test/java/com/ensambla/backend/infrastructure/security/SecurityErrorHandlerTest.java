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

    private final SecurityErrorHandler securityErrorHandler = new SecurityErrorHandler(JsonMapper.builder().build());

    @Test
    @DisplayName("Responde 401 JSON sin exponer la excepción interna")
    void commenceShouldReturnGeneric401() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        securityErrorHandler.commence(new MockHttpServletRequest("GET", "/api/auth/actual"), response,
                new InsufficientAuthenticationException("secreto"));

        thenCommenceShouldReturnGeneric401(response);
    }

    @Test
    @DisplayName("Responde 403 JSON sin exponer información interna del permiso")
    void handleShouldReturnGeneric403() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        securityErrorHandler.handle(new MockHttpServletRequest("POST", "/api/auth/login"), response,
                new AccessDeniedException("secreto"));

        thenHandleShouldReturnGeneric403(response);
    }

    // --- assert ---
    private void thenCommenceShouldReturnGeneric401(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(response.getContentAsString()).contains("Se requiere una sesión válida", "/api/auth/actual")
                .doesNotContain("secreto");
    }

    private void thenHandleShouldReturnGeneric403(MockHttpServletResponse response) throws Exception {
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("Acceso no permitido").doesNotContain("secreto");
    }
}
