package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocio;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse.Semana;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComoVieneElNegocioControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ObtenerComoVieneElNegocio obtenerComoVieneElNegocio;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        obtenerComoVieneElNegocio = mock(ObtenerComoVieneElNegocio.class);
        mockMvc = MockMvcDePrueba.standalone(new ComoVieneElNegocioController(obtenerComoVieneElNegocio))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve cómo viene el negocio del usuario de la sesión, con los nombres del contrato")
    void obtenerShouldReturnTheBusinessOfTheSessionUser() throws Exception {
        when(obtenerComoVieneElNegocio.execute(new ObtenerComoVieneElNegocioRequest(USUARIO_ID)))
                .thenReturn(new ObtenerComoVieneElNegocioResponse(true, 56100, 40500, 8000, 7600, 17000,
                        List.of(new Semana("Hace 1 sem.", 4100), new Semana("Hoy", 7600))));

        mockMvc.perform(get("/api/objetivos/negocio").principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayVentas").value(true))
                .andExpect(jsonPath("$.vendiste").value(56100))
                .andExpect(jsonPath("$.mercaderia").value(40500))
                .andExpect(jsonPath("$.gastos").value(8000))
                .andExpect(jsonPath("$.queda").value(7600))
                .andExpect(jsonPath("$.teDeben").value(17000))
                .andExpect(jsonPath("$.semanas[0].semana").value("Hace 1 sem."))
                .andExpect(jsonPath("$.semanas[1].queda").value(7600));
    }
}
