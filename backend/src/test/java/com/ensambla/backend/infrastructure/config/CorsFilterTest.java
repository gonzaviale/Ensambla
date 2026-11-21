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
    void prepararElFiltro() {
        filter = new CorsFilter();
        ReflectionTestUtils.setField(filter, "frontendUrl", FRONTEND_URL);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("Agrega los headers CORS cuando el origen es el del frontend")
    void agregaLosHeadersCorsAlOrigenDelFrontend() throws Exception {
        MockHttpServletRequest request = dadoUnPedidoDesde(FRONTEND_URL);

        cuandoPasaPorElFiltro(request);

        entoncesLaRespuestaAutorizaAlFrontend();
    }

    @Test
    @DisplayName("Continúa la cadena de filtros cuando el origen es el del frontend")
    void continuaLaCadenaConElOrigenDelFrontend() throws Exception {
        MockHttpServletRequest request = dadoUnPedidoDesde(FRONTEND_URL);

        cuandoPasaPorElFiltro(request);

        entoncesSiguioLaCadena(request);
    }

    @Test
    @DisplayName("No agrega autorización CORS cuando el origen es ajeno")
    void noAutorizaUnOrigenAjeno() throws Exception {
        MockHttpServletRequest request = dadoUnPedidoDesde(FOREIGN_ORIGIN);

        cuandoPasaPorElFiltro(request);

        entoncesLaRespuestaNoAutorizaElOrigen();
    }

    @Test
    @DisplayName("Continúa la cadena de filtros cuando el origen es ajeno")
    void continuaLaCadenaConUnOrigenAjeno() throws Exception {
        MockHttpServletRequest request = dadoUnPedidoDesde(FOREIGN_ORIGIN);

        cuandoPasaPorElFiltro(request);

        entoncesSiguioLaCadena(request);
    }

    @Test
    @DisplayName("Responde 200 al preflight sin invocar la cadena de filtros")
    void respondeElPreflightSinInvocarLaCadena() throws Exception {
        MockHttpServletRequest request = dadoUnPreflight();

        cuandoPasaPorElFiltro(request);

        entoncesElPreflightTerminoSinLaCadena();
    }

    private MockHttpServletRequest dadoUnPedidoDesde(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rutas");
        request.addHeader(ORIGIN_HEADER, origin);
        return request;
    }

    private MockHttpServletRequest dadoUnPreflight() {
        return new MockHttpServletRequest("OPTIONS", "/api/rutas");
    }

    private void cuandoPasaPorElFiltro(MockHttpServletRequest request) throws ServletException, IOException {
        filter.doFilter(request, response, chain);
    }

    private void entoncesLaRespuestaAutorizaAlFrontend() {
        assertThat(response.getHeader(ALLOW_ORIGIN_HEADER)).isEqualTo(FRONTEND_URL);
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Methods")).contains("GET", "OPTIONS");
        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .isEqualTo("Authorization, Content-Type, X-CSRF-TOKEN, X-Leido-En");
        assertThat(response.getHeader("Access-Control-Expose-Headers")).isEqualTo("X-Leido-En");
        assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("86400");
    }

    private void entoncesLaRespuestaNoAutorizaElOrigen() {
        assertThat(response.getHeader(ALLOW_ORIGIN_HEADER)).isNull();
    }

    private void entoncesSiguioLaCadena(MockHttpServletRequest request) throws ServletException, IOException {
        verify(chain).doFilter(request, response);
    }

    private void entoncesElPreflightTerminoSinLaCadena() {
        assertThat(response.getStatus()).isEqualTo(200);
        verifyNoInteractions(chain);
    }
}
