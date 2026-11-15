package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.MiniProgreso;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgresoResponse.PuntoSemanal;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.MiniObjetivoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ProgresoObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProgresoObjetivoControllerTest {

    private static final String RUTA = "/api/objetivos/" + OBJETIVO_ID + "/progreso";
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ObtenerProgreso obtenerProgreso = mock(ObtenerProgreso.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProgresoObjetivoController controller = new ProgresoObjetivoController(
                obtenerProgreso, new ProgresoObjetivoPresentationMapper(new MiniObjetivoPresentationMapper()));
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve la meta, el hito y la serie semanal del objetivo y de sus mini-objetivos")
    void obtenerShouldReturnTheMappedProgress() throws Exception {
        when(obtenerProgreso.execute(any())).thenReturn(new ObtenerProgresoResponse(
                META,
                HitoObjetivo.OBJETIVO_ALCANZADO,
                List.of(new PuntoSemanal(SEMANA, new BigDecimal("-27000"), true)),
                List.of(new MiniProgreso(MiniObjetivoItem.de(miniObjetivo()), HitoObjetivo.SIN_AVANCE,
                        List.of(new PuntoSemanal(SEMANA, new BigDecimal("12600"), false))))));

        ResultActions result = mockMvc.perform(get(RUTA).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").value(400000.0))
                .andExpect(jsonPath("$.hito").value("OBJETIVO_ALCANZADO"))
                .andExpect(jsonPath("$.semanas[0].semana").value("2026-11-09"))
                .andExpect(jsonPath("$.semanas[0].valor").value(-27000.0))
                .andExpect(jsonPath("$.semanas[0].estimado").value(true))
                .andExpect(jsonPath("$.minis[0].mini.id").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.minis[0].mini.titulo").value(TITULO))
                .andExpect(jsonPath("$.minis[0].hito").value("SIN_AVANCE"))
                .andExpect(jsonPath("$.minis[0].semanas[0].valor").value(12600.0))
                .andExpect(jsonPath("$.minis[0].semanas[0].estimado").value(false));
    }

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void obtenerShouldPassTheSessionUserAndTheObjetivo() throws Exception {
        when(obtenerProgreso.execute(any())).thenReturn(new ObtenerProgresoResponse(META, null, List.of(), List.of()));

        mockMvc.perform(get(RUTA).principal(principal)).andExpect(status().isOk());

        ArgumentCaptor<ObtenerProgresoRequest> captor = ArgumentCaptor.forClass(ObtenerProgresoRequest.class);
        verify(obtenerProgreso).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("GET responde 200 con listas vacías cuando todavía no hay nada medido")
    void obtenerShouldReturnEmptyListsWhenNothingWasMeasured() throws Exception {
        when(obtenerProgreso.execute(any())).thenReturn(new ObtenerProgresoResponse(META, null, List.of(), List.of()));

        ResultActions result = mockMvc.perform(get(RUTA).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas").isEmpty())
                .andExpect(jsonPath("$.minis").isEmpty())
                .andExpect(jsonPath("$.hito").doesNotExist());
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void obtenerShouldReturn404WhenTheObjetivoIsNotFound() throws Exception {
        when(obtenerProgreso.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        ResultActions result = mockMvc.perform(get(RUTA).principal(principal));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void obtenerShouldReturn400WhenTheIdIsNotAUuid() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/objetivos/no-es-un-uuid/progreso").principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(obtenerProgreso);
    }
}
