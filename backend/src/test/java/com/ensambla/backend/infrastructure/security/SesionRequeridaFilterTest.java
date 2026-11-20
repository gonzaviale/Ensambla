package com.ensambla.backend.infrastructure.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SesionRequeridaFilterTest {

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final FilterChain chain = mock(FilterChain.class);
    private final SesionRequeridaFilter filter =
            new SesionRequeridaFilter(ignored -> false, new SecurityErrorHandler(new ObjectMapper()));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sin identidad rechaza con 401 sin ejecutar el endpoint")
    void sinIdentidadRechazaCon401SinEjecutarElEndpoint() throws Exception {
        filtrar();

        entoncesUnauthorized();
    }

    @Test
    @DisplayName("Rechaza una identidad que todavía no fue autenticada")
    void rechazaUnaIdentidadQueTodaviaNoFueAutenticada() throws Exception {
        dadoAuthentication(UsernamePasswordAuthenticationToken.unauthenticated("persona", "clave"));

        filtrar();

        entoncesUnauthorized();
    }

    @Test
    @DisplayName("Rechaza la identidad anónima")
    void rechazaLaIdentidadAnonima() throws Exception {
        dadoAuthentication(new AnonymousAuthenticationToken("key", "anonymous",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        filtrar();

        entoncesUnauthorized();
    }

    @Test
    @DisplayName("Una identidad autenticada continúa a la protección CSRF")
    void unaIdentidadAutenticadaContinuaALaProteccionCSRF() throws Exception {
        dadoAuthentication(UsernamePasswordAuthenticationToken.authenticated("persona", null, List.of()));

        filtrar();

        verify(chain).doFilter(request, response);
    }

    @Test
    @DisplayName("Las rutas públicas no requieren identidad")
    void lasRutasPublicasNoRequierenIdentidad() throws Exception {
        SesionRequeridaFilter publicFilter =
                new SesionRequeridaFilter(ignored -> true, new SecurityErrorHandler(new ObjectMapper()));

        publicFilter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    // --- dado ---
    private void dadoAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // --- cuando ---
    private void filtrar() throws Exception {
        filter.doFilter(request, response, chain);
    }

    // --- entonces ---
    private void entoncesUnauthorized() {
        assertThat(response.getStatus()).isEqualTo(401);
        verifyNoInteractions(chain);
    }
}

