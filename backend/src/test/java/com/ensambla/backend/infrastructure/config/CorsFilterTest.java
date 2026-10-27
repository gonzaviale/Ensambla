package com.ensambla.backend.infrastructure.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CorsFilterTest {

    private static final String FRONTEND_URL = "http://localhost:5173";
    private static final String FOREIGN_ORIGIN = "http://example.org";
    private static final String ORIGIN_HEADER = "Origin";
    private static final String ALLOW_ORIGIN_HEADER = "Access-Control-Allow-Origin";

    @Mock
    private FilterChain chain;

    private CorsFilter filter;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new CorsFilter();
        ReflectionTestUtils.setField(filter, "frontendUrl", FRONTEND_URL);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Agrega los headers CORS cuando el origen es el del frontend")
    void doFilterShouldAddCorsHeadersWhenOriginIsAllowed() throws Exception {
        MockHttpServletRequest request = requestFrom(FRONTEND_URL);

        filter(request);

        thenResponseHasCorsHeaders();
    }

    @Test
    @DisplayName("Continúa la cadena de filtros cuando el origen es el del frontend")
    void doFilterShouldContinueChainWhenOriginIsAllowed() throws Exception {
        MockHttpServletRequest request = requestFrom(FRONTEND_URL);

        filter(request);

        thenChainContinued(request);
    }

    @Test
    @DisplayName("No agrega autorización CORS cuando el origen es ajeno")
    void doFilterShouldNotAddCorsHeadersWhenOriginIsForeign() throws Exception {
        MockHttpServletRequest request = requestFrom(FOREIGN_ORIGIN);

        filter(request);

        thenResponseHasNoAllowedOrigin();
    }

    @Test
    @DisplayName("Continúa la cadena de filtros cuando el origen es ajeno")
    void doFilterShouldContinueChainWhenOriginIsForeign() throws Exception {
        MockHttpServletRequest request = requestFrom(FOREIGN_ORIGIN);

        filter(request);

        thenChainContinued(request);
    }

    @Test
    @DisplayName("Responde 200 al preflight sin invocar la cadena de filtros")
    void doFilterShouldReturnOkWithoutChainWhenRequestIsPreflight() throws Exception {
        MockHttpServletRequest request = preflightRequest();

        filter(request);

        thenPreflightEndedWithoutChain();
    }

    // --- arrange ---
    private MockHttpServletRequest requestFrom(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rutas");
        request.addHeader(ORIGIN_HEADER, origin);
        return request;
    }

    private MockHttpServletRequest preflightRequest() {
        return new MockHttpServletRequest("OPTIONS", "/api/rutas");
    }

    // --- act ---
    private void filter(MockHttpServletRequest request) throws ServletException, IOException {
        filter.doFilter(request, response, chain);
    }

    // --- assert ---
    private void thenResponseHasCorsHeaders() {
        assertThat(response.getHeader(ALLOW_ORIGIN_HEADER)).isEqualTo(FRONTEND_URL);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("GET", "OPTIONS");
        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .isEqualTo("Authorization, Content-Type, X-CSRF-TOKEN, X-Leido-En");
        assertThat(response.getHeader("Access-Control-Expose-Headers")).isEqualTo("X-Leido-En");
        assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("86400");
    }

    private void thenResponseHasNoAllowedOrigin() {
        assertThat(response.getHeader(ALLOW_ORIGIN_HEADER)).isNull();
    }

    private void thenChainContinued(MockHttpServletRequest request) throws ServletException, IOException {
        verify(chain).doFilter(request, response);
    }

    private void thenPreflightEndedWithoutChain() {
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(chain);
    }
}
