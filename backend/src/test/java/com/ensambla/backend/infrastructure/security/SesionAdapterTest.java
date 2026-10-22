package com.ensambla.backend.infrastructure.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;

class SesionAdapterTest {

    private final SesionAdapter sesionAdapter = new SesionAdapter();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Rota una sesión existente y persiste solo identidad y rol")
    void iniciarShouldRotateSessionAndPersistIdentity() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        String previousId = session.getId();
        sesionAdapter.iniciar(USUARIO_ID, "PERSONA", request, new MockHttpServletResponse());

        thenIniciarShouldRotateSessionAndPersistIdentity(session, previousId);
    }

    @Test
    @DisplayName("Crea una sesión autenticada si no había una sesión previa")
    void iniciarShouldCreateSessionWhenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        sesionAdapter.iniciar(USUARIO_ID, "PERSONA", request, new MockHttpServletResponse());

        thenIniciarShouldCreateSessionWhenMissing(request);
    }

    @Test
    @DisplayName("Invalida la sesión y elimina la cookie al cerrar el acceso")
    void cerrarShouldInvalidateSessionAndClearCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        sesionAdapter.cerrar(request, response);

        thenCerrarShouldInvalidateSessionAndClearCookie(session, response);
    }

    @Test
    @DisplayName("Cerrar una sesión inexistente no produce error")
    void cerrarShouldBeIdempotentWhenSessionIsMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        sesionAdapter.cerrar(request, new MockHttpServletResponse());

        thenCerrarShouldBeIdempotentWhenSessionIsMissing(request);
    }

    // --- assert ---
    private void thenIniciarShouldRotateSessionAndPersistIdentity(MockHttpSession session, String previousId) {
        assertThat(session.getId()).isNotEqualTo(previousId);
        SecurityContext context = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(context.getAuthentication().getName()).isEqualTo(USUARIO_ID.toString());
        assertThat(context.getAuthentication().getCredentials()).isNull();
        assertThat(context.getAuthentication().getAuthorities()).extracting("authority")
                .containsExactly("ROLE_PERSONA");
    }

    private void thenIniciarShouldCreateSessionWhenMissing(MockHttpServletRequest request) {
        assertThat(request.getSession(false)).isNotNull();
    }

    private void thenCerrarShouldInvalidateSessionAndClearCookie(
            MockHttpSession session, MockHttpServletResponse response) {
        assertThat(session.isInvalid()).isTrue();
        assertThat(response.getCookie("ENSAMBLA_SESSION").getMaxAge()).isZero();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void thenCerrarShouldBeIdempotentWhenSessionIsMissing(MockHttpServletRequest request) {
        assertThat(request.getSession(false)).isNull();
    }
}
