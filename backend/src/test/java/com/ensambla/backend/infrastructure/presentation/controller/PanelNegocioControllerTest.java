package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocio;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocioResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.reporte.PanelNegocioPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PanelNegocioControllerTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 7);

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private ObtenerPanelNegocio obtenerPanelNegocio;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        obtenerPanelNegocio = mock(ObtenerPanelNegocio.class);
        mockMvc = MockMvcDePrueba.standalone(
                        new PanelNegocioController(obtenerPanelNegocio, new PanelNegocioPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve las métricas y el flujo de caja, y pasa el sistema y los días con el usuario de la sesión")
    void obtenerShouldReturnThePanel() throws Exception {
        when(obtenerPanelNegocio.execute(any(ObtenerPanelNegocioRequest.class))).thenReturn(
                new ObtenerPanelNegocioResponse(DESDE, HASTA, 7, true,
                        new ObtenerPanelNegocioResponse.Metricas(new BigDecimal("1000"), null, null,
                                new BigDecimal("1000"), BigDecimal.ZERO, null, new BigDecimal("250"), null),
                        List.of(new ObtenerPanelNegocioResponse.Semana(DESDE, HASTA, new BigDecimal("1000"),
                                BigDecimal.ZERO, new BigDecimal("1000"), new BigDecimal("1000")))));

        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", PROYECTO_ID.toString())
                        .param("dias", "7").principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayDatos").value(true))
                .andExpect(jsonPath("$.dias").value(7))
                .andExpect(jsonPath("$.metricas.vendiste").value(1000))
                .andExpect(jsonPath("$.metricas.gastos").doesNotExist())
                .andExpect(jsonPath("$.flujoDeCaja[0].saldoAcumulado").value(1000));
        verify(obtenerPanelNegocio).execute(new ObtenerPanelNegocioRequest(USUARIO_ID, PROYECTO_ID, 7));
    }

    @Test
    @DisplayName("GET de un sistema sin ventas devuelve el panel vacío")
    void obtenerShouldReturnAnEmptyPanel() throws Exception {
        when(obtenerPanelNegocio.execute(any(ObtenerPanelNegocioRequest.class))).thenReturn(
                new ObtenerPanelNegocioResponse(DESDE, HASTA, 7, false, null, List.of()));

        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", PROYECTO_ID.toString()).principal(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hayDatos").value(false))
                .andExpect(jsonPath("$.flujoDeCaja").isEmpty());
        verify(obtenerPanelNegocio).execute(new ObtenerPanelNegocioRequest(USUARIO_ID, PROYECTO_ID, null));
    }

    @Test
    @DisplayName("GET de un sistema ajeno responde 404")
    void obtenerShouldReturn404ForForeignProject() throws Exception {
        when(obtenerPanelNegocio.execute(any(ObtenerPanelNegocioRequest.class)))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", PROYECTO_ID.toString()).principal(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET con días inválidos o un id mal escrito responde 400")
    void obtenerShouldReturn400ForInvalidParameters() throws Exception {
        when(obtenerPanelNegocio.execute(any(ObtenerPanelNegocioRequest.class)))
                .thenThrow(new BusinessRuleException("La cantidad de días tiene que ser mayor a cero"));

        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", PROYECTO_ID.toString())
                        .param("dias", "0").principal(sesion))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/negocio/panel").param("proyectoId", "no-es-un-id").principal(sesion))
                .andExpect(status().isBadRequest());
    }
}
