package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidos;
import com.ensambla.backend.domain.useCase.plan.desbloquearContenidos.DesbloquearContenidosRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContenidoPlanControllerTest {

    private static final UUID USUARIO = UUID.randomUUID();
    private final DesbloquearContenidos caso = mock(DesbloquearContenidos.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ContenidoPlanController(caso))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("El desbloqueo usa el usuario autenticado y devuelve sólo las claves admitidas")
    void desbloquearShouldUseAuthenticatedIdentity() throws Exception {
        var pedido = new DesbloquearContenidosRequest(USUARIO, "AVISOS", List.of("a"));
        when(caso.execute(pedido)).thenReturn(new AccesoContenidoItem(List.of("a"), 1, 3, 1, 0,
                OffsetDateTime.parse("2026-11-01T00:00:00Z")));

        mvc.perform(post("/api/planes/contenidos")
                        .principal(UsernamePasswordAuthenticationToken.authenticated(USUARIO.toString(), null, List.of()))
                        .contentType("application/json").content("{\"tipo\":\"AVISOS\",\"claves\":[\"a\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usadas").value(1))
                .andExpect(jsonPath("$.autorizadas[0]").value("a"));

        verify(caso).execute(pedido);
    }

    @Test
    @DisplayName("Un tipo desconocido no llega al caso de uso")
    void desbloquearShouldRejectInvalidType() throws Exception {
        mvc.perform(post("/api/planes/contenidos").contentType("application/json")
                        .content("{\"tipo\":\"OTRO\",\"claves\":[]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(caso);
    }

    @Test
    @DisplayName("Una lista nula no llega al caso de uso")
    void desbloquearShouldRejectNullKeys() throws Exception {
        mvc.perform(post("/api/planes/contenidos").contentType("application/json")
                        .content("{\"tipo\":\"AVISOS\",\"claves\":null}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(caso);
    }
}
