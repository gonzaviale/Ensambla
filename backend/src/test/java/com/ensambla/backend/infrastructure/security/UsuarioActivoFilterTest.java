package com.ensambla.backend.infrastructure.security;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import com.ensambla.backend.domain.useCase.auth.obtenerSesion.ObtenerSesion;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class UsuarioActivoFilterTest {

    private ObtenerSesion obtenerSesion;

    private SesionAdapter sesionAdapter;

    private SecurityErrorHandler securityErrorHandler;

    private UsuarioActivoFilter usuarioActivoFilter;

    private FilterChain chain;

    private MockHttpServletRequest request;

    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        obtenerSesion = mock(ObtenerSesion.class);
        sesionAdapter = mock(SesionAdapter.class);
        securityErrorHandler = mock(SecurityErrorHandler.class);
        usuarioActivoFilter = new UsuarioActivoFilter(obtenerSesion, sesionAdapter, securityErrorHandler);
        chain = mock(FilterChain.class);
        request = new MockHttpServletRequest("GET", "/api/auth/actual");
        response = new MockHttpServletResponse();
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("No consulta usuarios en una petición anónima")
    void noConsultaUsuariosEnUnaPeticionAnonima() throws Exception {
        dadoUnaPeticionAnonima();

        cuandoPasaPorElFiltro();

        entoncesLaPeticionContinuaSinConsultarUsuarios();
    }

    @Test
    @DisplayName("Verifica que la identidad autenticada siga activa antes de continuar")
    void verificaQueLaIdentidadAutenticadaSigaActivaAntesDeContinuar() throws Exception {
        dadoUnaPeticionDeUnUsuarioActivo();

        cuandoPasaPorElFiltro();

        entoncesSeVerificaElUsuarioYLaPeticionContinua();
    }

    @Test
    @DisplayName("Invalida la sesión de usuario suspendido y detiene la petición con 401")
    void invalidaLaSesionDeUsuarioSuspendidoYDetieneLaPeticionCon401() throws Exception {
        dadoUnaPeticionDeUnUsuarioSuspendido();

        cuandoPasaPorElFiltro();

        entoncesSeCierraLaSesionYSeResponde401SinContinuar();
    }

    // --- dado ---
    private void dadoUnaPeticionAnonima() {
        SecurityContextHolder.clearContext();
    }

    private void dadoUnaPeticionDeUnUsuarioActivo() {
        dadoUnaPeticionAutenticada();
        when(obtenerSesion.execute(USUARIO_ID)).thenReturn(UsuarioResponse.de(usuario()));
    }

    private void dadoUnaPeticionDeUnUsuarioSuspendido() {
        dadoUnaPeticionAutenticada();
        when(obtenerSesion.execute(USUARIO_ID)).thenThrow(new CredencialesInvalidasException());
    }

    private void dadoUnaPeticionAutenticada() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.unauthenticated(USUARIO_ID.toString(), null));
    }

    // --- cuando ---
    private void cuandoPasaPorElFiltro() throws Exception {
        usuarioActivoFilter.doFilter(request, response, chain);
    }

    // --- entonces ---
    private void entoncesLaPeticionContinuaSinConsultarUsuarios() throws Exception {
        verify(chain).doFilter(request, response);
        verifyNoInteractions(obtenerSesion, sesionAdapter, securityErrorHandler);
    }

    private void entoncesSeVerificaElUsuarioYLaPeticionContinua() throws Exception {
        verify(obtenerSesion).execute(USUARIO_ID);
        verify(chain).doFilter(request, response);
    }

    private void entoncesSeCierraLaSesionYSeResponde401SinContinuar() throws Exception {
        verify(sesionAdapter).cerrar(request, response);
        verify(securityErrorHandler).commence(any(), any(), any());
        verifyNoInteractions(chain);
    }
}
