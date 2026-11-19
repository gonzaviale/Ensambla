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

    private MockHttpServletRequest peticion = new MockHttpServletRequest();

    private final MockHttpServletResponse respuesta = new MockHttpServletResponse();

    private MockHttpSession sesion;

    private String idAnterior;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Rota una sesión existente y persiste solo identidad y rol")
    void rotaUnaSesionExistenteYPersisteSoloIdentidadYRol() {
        dadoUnaPeticionConSesionAbierta();

        cuandoIniciaSesionUnaPersona();

        entoncesLaSesionRotaYGuardaSoloLaIdentidadYElRol();
    }

    @Test
    @DisplayName("Crea una sesión autenticada si no había una sesión previa")
    void creaUnaSesionAutenticadaSiNoHabiaUnaSesionPrevia() {
        dadoUnaPeticionSinSesion();

        cuandoIniciaSesionUnaPersona();

        entoncesQuedaUnaSesionAbierta();
    }

    @Test
    @DisplayName("Invalida la sesión y elimina la cookie al cerrar el acceso")
    void invalidaLaSesionYEliminaLaCookieAlCerrarElAcceso() {
        dadoUnaPeticionConSesionAbierta();

        cuandoCierraSesionLaPersona();

        entoncesLaSesionQuedaInvalidaSinCookie();
    }

    @Test
    @DisplayName("Cerrar una sesión inexistente no produce error")
    void cerrarUnaSesionInexistenteNoProduceError() {
        dadoUnaPeticionSinSesion();

        cuandoCierraSesionLaPersona();

        entoncesNoQuedaNingunaSesion();
    }

    // --- dado ---
    private void dadoUnaPeticionConSesionAbierta() {
        sesion = new MockHttpSession();
        idAnterior = sesion.getId();
        peticion.setSession(sesion);
    }

    private void dadoUnaPeticionSinSesion() {
        peticion = new MockHttpServletRequest();
    }

    // --- cuando ---
    private void cuandoIniciaSesionUnaPersona() {
        sesionAdapter.iniciar(USUARIO_ID, "PERSONA", peticion, respuesta);
    }

    private void cuandoCierraSesionLaPersona() {
        sesionAdapter.cerrar(peticion, respuesta);
    }

    // --- entonces ---
    private void entoncesLaSesionRotaYGuardaSoloLaIdentidadYElRol() {
        assertThat(sesion.getId()).isNotEqualTo(idAnterior);
        SecurityContext contexto = (SecurityContext) sesion.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(contexto.getAuthentication().getName()).isEqualTo(USUARIO_ID.toString());
        assertThat(contexto.getAuthentication().getCredentials()).isNull();
        assertThat(contexto.getAuthentication().getAuthorities()).extracting("authority")
                .containsExactly("ROLE_PERSONA");
    }

    private void entoncesQuedaUnaSesionAbierta() {
        assertThat(peticion.getSession(false)).isNotNull();
    }

    private void entoncesLaSesionQuedaInvalidaSinCookie() {
        assertThat(sesion.isInvalid()).isTrue();
        assertThat(respuesta.getCookie("ENSAMBLA_SESSION").getMaxAge()).isZero();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private void entoncesNoQuedaNingunaSesion() {
        assertThat(peticion.getSession(false)).isNull();
    }
}
