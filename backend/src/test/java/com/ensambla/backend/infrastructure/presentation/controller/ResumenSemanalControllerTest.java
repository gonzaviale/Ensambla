package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.TipoReajuste;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanas;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ManualItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.DecisionesItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.MiniItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.ReajusteItem;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanasResponse.SemanaItem;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ResumenSemanalPresentationMapper;
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
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
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

class ResumenSemanalControllerTest {

    private static final List<ManualItem> MANUALES = List.of(
            new ManualItem(MINI_ID, "Pedile a 3 clientes", HizoManual.SI, QuePaso.FUNCIONO),
            new ManualItem(OTRO_MINI_ID, "Un cartel", null, null));

    private static final String RUTA = "/api/objetivos/" + OBJETIVO_ID + "/semanas";
    private static final String RUTA_CON_ID_INVALIDO = "/api/objetivos/id-invalido/semanas";
    private static final LocalDate SEMANA = LocalDate.of(2026, 11, 9);

    private final ListarSemanas listarSemanas = mock(ListarSemanas.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;
    private ResultActions resultado;

    @BeforeEach
    void setUp() {
        ResumenSemanalController controller =
                new ResumenSemanalController(listarSemanas, new ResumenSemanalPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET devuelve el último cierre y cada semana con sus decisiones y reajustes")
    void listarShouldReturnTheMappedWeeks() throws Exception {
        givenUnaSemanaCerradaConUnReajuste();

        whenSePidenLasSemanas(RUTA);

        thenSeDevuelveLaSemanaConSusDecisionesYReajustes();
    }

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void listarShouldPassTheSessionUserAndTheObjetivo() throws Exception {
        givenNingunaSemanaCerrada();

        whenSePidenLasSemanas(RUTA);

        thenElCasoDeUsoRecibeElUsuarioYElObjetivo();
    }

    @Test
    @DisplayName("GET responde 200 sin semanas cuando todavía no se cerró ninguna")
    void listarShouldReturnNoWeeksWhenNoneWasClosed() throws Exception {
        givenNingunaSemanaCerrada();

        whenSePidenLasSemanas(RUTA);

        thenSeResponde200SinSemanas();
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void listarShouldReturn404WhenTheObjetivoIsNotFound() throws Exception {
        givenUnObjetivoInexistente();

        whenSePidenLasSemanas(RUTA);

        thenSeResponde404();
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void listarShouldReturn400WhenTheIdIsNotUuid() throws Exception {
        whenSePidenLasSemanas(RUTA_CON_ID_INVALIDO);

        thenSeResponde400();
        thenNoLlegaAlCasoDeUso();
    }

    // --- arrange ---
    private void givenUnaSemanaCerradaConUnReajuste() {
        ReajusteItem reajuste = new ReajusteItem(TipoReajuste.SUBIR_META, "Lo lograste",
                new MiniItem("Vender más", new BigDecimal("100")), new MiniItem("Vender más", new BigDecimal("110")));
        when(listarSemanas.execute(any())).thenReturn(new ListarSemanasResponse(SEMANA, List.of(new SemanaItem(
                SEMANA, new BigDecimal("180000"), new BigDecimal("0.45"), new DecisionesItem(1, 2, 3, 4),
                "Esta semana…", false, List.of(reajuste), MANUALES))));
    }

    private void givenNingunaSemanaCerrada() {
        when(listarSemanas.execute(any())).thenReturn(new ListarSemanasResponse(null, List.of()));
    }

    private void givenUnObjetivoInexistente() {
        when(listarSemanas.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- act ---
    private void whenSePidenLasSemanas(String ruta) throws Exception {
        resultado = mockMvc.perform(get(ruta).principal(principal));
    }

    // --- assert ---
    private void thenSeDevuelveLaSemanaConSusDecisionesYReajustes() throws Exception {
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoCierre").value("2026-11-09"))
                .andExpect(jsonPath("$.semanas[0].semana").value("2026-11-09"))
                .andExpect(jsonPath("$.semanas[0].resultado").value(180000))
                .andExpect(jsonPath("$.semanas[0].manuales[0].miniObjetivoId").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.semanas[0].manuales[0].titulo").value("Pedile a 3 clientes"))
                .andExpect(jsonPath("$.semanas[0].manuales[0].hizo").value("SI"))
                .andExpect(jsonPath("$.semanas[0].manuales[0].quePaso").value("FUNCIONO"))
                .andExpect(jsonPath("$.semanas[0].manuales[1].hizo").doesNotExist())
                .andExpect(jsonPath("$.semanas[0].avance").value(0.45))
                .andExpect(jsonPath("$.semanas[0].decisiones.aplicadas").value(1))
                .andExpect(jsonPath("$.semanas[0].decisiones.descartadas").value(2))
                .andExpect(jsonPath("$.semanas[0].decisiones.aceptadas").value(3))
                .andExpect(jsonPath("$.semanas[0].decisiones.ignoradas").value(4))
                .andExpect(jsonPath("$.semanas[0].queFunciono").value("Esta semana…"))
                .andExpect(jsonPath("$.semanas[0].textoConIa").value(false))
                .andExpect(jsonPath("$.semanas[0].reajustes[0].tipo").value("SUBIR_META"))
                .andExpect(jsonPath("$.semanas[0].reajustes[0].motivo").value("Lo lograste"))
                .andExpect(jsonPath("$.semanas[0].reajustes[0].de.titulo").value("Vender más"))
                .andExpect(jsonPath("$.semanas[0].reajustes[0].de.meta").value(100))
                .andExpect(jsonPath("$.semanas[0].reajustes[0].a.meta").value(110));
    }

    private void thenElCasoDeUsoRecibeElUsuarioYElObjetivo() throws Exception {
        resultado.andExpect(status().isOk());
        ArgumentCaptor<ListarSemanasRequest> captor = ArgumentCaptor.forClass(ListarSemanasRequest.class);
        verify(listarSemanas).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void thenSeResponde200SinSemanas() throws Exception {
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.ultimoCierre").doesNotExist())
                .andExpect(jsonPath("$.semanas").isEmpty());
    }

    private void thenSeResponde404() throws Exception {
        resultado.andExpect(status().isNotFound());
    }

    private void thenSeResponde400() throws Exception {
        resultado.andExpect(status().isBadRequest());
    }

    private void thenNoLlegaAlCasoDeUso() {
        verifyNoInteractions(listarSemanas);
    }
}
