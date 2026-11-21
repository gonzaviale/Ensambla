package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidosRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContenidoPlanControllerTest {

    private static final UsuarioId USUARIO = UsuarioId.nuevo();
    private static final OffsetDateTime VENCE = OffsetDateTime.parse("2026-11-01T00:00:00Z");

    private final DesbloquearContenidos caso = mock(DesbloquearContenidos.class);
    private final MockMvc mvc = MockMvcDePrueba.standalone(new ContenidoPlanController(caso))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    private ResultActions respuesta;

    @Test
    @DisplayName("El desbloqueo usa el usuario autenticado y devuelve sólo las claves admitidas")
    void elDesbloqueoUsaElUsuarioAutenticadoYDevuelveSoloLasClavesAdmitidas() throws Exception {
        dadoQueSeAdmitenLasClaves(List.of("a"), 1, 3, 1);

        cuandoSeDesbloqueanComoUsuarioAutenticado("{\"tipo\":\"AVISOS\",\"claves\":[\"a\"]}");

        entoncesSeDevuelveUnaClaveUsadaYAutorizada();
        entoncesElCasoDeUsoRecibeLasClaves(List.of("a"));
    }

    @Test
    @DisplayName("Los avisos del servidor llegan al caso de uso sin el prefijo y vuelven con el que se pidió")
    void losAvisosDelServidorLleganAlCasoDeUsoSinElPrefijoYVuelvenConElQueSePidio() throws Exception {
        dadoQueSeAdmitenLasClaves(List.of("a1", "demo:1"), 2, 6, 2);

        cuandoSeDesbloqueanComoUsuarioAutenticado("{\"tipo\":\"AVISOS\",\"claves\":[\"servidor:a1\",\"demo:1\"]}");

        entoncesVuelvenLasClavesConElPrefijoPedido();
    }

    @Test
    @DisplayName("Un tipo desconocido no llega al caso de uso")
    void unTipoDesconocidoNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeDesbloqueanSinSesion("{\"tipo\":\"OTRO\",\"claves\":[]}");

        entoncesSeRechazaSinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("Una lista nula no llega al caso de uso")
    void unaListaNulaNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeDesbloqueanSinSesion("{\"tipo\":\"AVISOS\",\"claves\":null}");

        entoncesSeRechazaSinLlegarAlCasoDeUso();
    }

    // --- dado ---

    private void dadoQueSeAdmitenLasClaves(List<String> claves, int usadas, int limite, int restantes) {
        when(caso.execute(pedidoDe(claves))).thenReturn(
                new AccesoContenidoItem(claves, usadas, limite, restantes, 0, VENCE));
    }

    // --- cuando ---

    private void cuandoSeDesbloqueanComoUsuarioAutenticado(String cuerpo) throws Exception {
        respuesta = mvc.perform(post("/api/planes/contenidos")
                .principal(UsernamePasswordAuthenticationToken.authenticated(USUARIO.toString(), null, List.of()))
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeDesbloqueanSinSesion(String cuerpo) throws Exception {
        respuesta = mvc.perform(post("/api/planes/contenidos").contentType("application/json").content(cuerpo));
    }

    // --- entonces ---

    private void entoncesSeDevuelveUnaClaveUsadaYAutorizada() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.usadas").value(1))
                .andExpect(jsonPath("$.autorizadas[0]").value("a"));
    }

    private void entoncesElCasoDeUsoRecibeLasClaves(List<String> claves) {
        verify(caso).execute(pedidoDe(claves));
    }

    private void entoncesVuelvenLasClavesConElPrefijoPedido() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.autorizadas[0]").value("servidor:a1"))
                .andExpect(jsonPath("$.autorizadas[1]").value("demo:1"));
    }

    private void entoncesSeRechazaSinLlegarAlCasoDeUso() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(caso);
    }

    // --- datos ---

    private DesbloquearContenidosRequest pedidoDe(List<String> claves) {
        return new DesbloquearContenidosRequest(USUARIO, "AVISOS", claves);
    }
}
