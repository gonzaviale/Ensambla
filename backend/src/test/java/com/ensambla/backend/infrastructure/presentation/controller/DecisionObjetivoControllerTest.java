package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.DecisionPaso;
import com.ensambla.backend.domain.model.objetivo.EfectoDecision;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.useCase.objetivo.DecisionItem;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisiones;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisionesResponse.SemanaDecisiones;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPaso;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPasoRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.DecisionObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.DECISION_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.TITULO_AVISO;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DecisionObjetivoControllerTest {

    private static final String BASE = "/api/objetivos/" + OBJETIVO_ID;
    private static final String PASO = BASE + "/mini-objetivos/" + MINI_ID + "/decisiones";
    private static final String DECISIONES = BASE + "/decisiones";

    private final RegistrarDecisionPaso registrarDecisionPaso = mock(RegistrarDecisionPaso.class);
    private final ListarDecisiones listarDecisiones = mock(ListarDecisiones.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DecisionObjetivoController controller = new DecisionObjetivoController(
                registrarDecisionPaso, listarDecisiones, new DecisionObjetivoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST anota la decisión del paso y responde 201 con todos sus datos")
    void decidirPasoShouldReturnCreatedWithTheDecision() throws Exception {
        when(registrarDecisionPaso.execute(any())).thenReturn(
                DecisionItem.de(decision(TipoDecision.PASO_ACEPTADO, null), EfectoDecision.SIN_MEDICION));

        ResultActions result = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ACEPTADO\"}"));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DECISION_ID.toString()))
                .andExpect(jsonPath("$.tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.miniObjetivoId").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.descripcion").value(TITULO_AVISO))
                .andExpect(jsonPath("$.semanaInicio").value("2026-11-09"))
                .andExpect(jsonPath("$.efecto").value("SIN_MEDICION"));
    }

    @Test
    @DisplayName("POST manda al caso de uso el usuario de la sesión, el objetivo, el mini-objetivo y la respuesta")
    void decidirPasoShouldPassTheRequestToTheUseCase() throws Exception {
        when(registrarDecisionPaso.execute(any())).thenReturn(
                DecisionItem.de(decision(TipoDecision.PASO_IGNORADO, null), EfectoDecision.SIN_MEDICION));

        mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"IGNORADO\"}"))
                .andExpect(status().isCreated());

        ArgumentCaptor<RegistrarDecisionPasoRequest> captor =
                ArgumentCaptor.forClass(RegistrarDecisionPasoRequest.class);
        verify(registrarDecisionPaso).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().decision()).isEqualTo(DecisionPaso.IGNORADO);
    }

    @Test
    @DisplayName("POST responde 400 cuando falta la decisión y no llega al caso de uso")
    void decidirPasoShouldReturn400WhenTheDecisionIsMissing() throws Exception {
        ResultActions result = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").value("La decisión es obligatoria"));
        verifyNoInteractions(registrarDecisionPaso);
    }

    @Test
    @DisplayName("POST responde 400 cuando la decisión no es una de las permitidas")
    void decidirPasoShouldReturn400WhenTheDecisionIsUnknown() throws Exception {
        ResultActions result = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"AVISO_APLICADO\"}"));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(registrarDecisionPaso);
    }

    @Test
    @DisplayName("POST responde 404 cuando el mini-objetivo es de otro negocio o no existe")
    void decidirPasoShouldReturn404WhenTheMiniObjetivoIsNotFound() throws Exception {
        when(registrarDecisionPaso.execute(any()))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));

        ResultActions result = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ACEPTADO\"}"));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST responde 400 cuando el mini-objetivo ya no está en curso")
    void decidirPasoShouldReturn400WhenTheMiniObjetivoIsNoLongerInProgress() throws Exception {
        when(registrarDecisionPaso.execute(any()))
                .thenThrow(new BusinessRuleException("El mini-objetivo ya no está en curso"));

        ResultActions result = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"ACEPTADO\"}"));

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET lista las decisiones por semana con su efecto")
    void listarShouldReturnTheWeeksWithTheirDecisions() throws Exception {
        DecisionItem item = DecisionItem.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                new EfectoDecision(EstadoEfecto.MEDIDO, new BigDecimal("350.00")));
        when(listarDecisiones.execute(any()))
                .thenReturn(new ListarDecisionesResponse(List.of(new SemanaDecisiones(SEMANA, List.of(item)))));

        ResultActions result = mockMvc.perform(get(DECISIONES).principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].semanaInicio").value("2026-11-09"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_APLICADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].avisoId").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MEDIDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").value(350.0));
    }

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void listarShouldPassTheSessionUserAndTheObjetivo() throws Exception {
        when(listarDecisiones.execute(any())).thenReturn(new ListarDecisionesResponse(List.of()));

        mockMvc.perform(get(DECISIONES).principal(principal)).andExpect(status().isOk());

        ArgumentCaptor<ListarDecisionesRequest> captor = ArgumentCaptor.forClass(ListarDecisionesRequest.class);
        verify(listarDecisiones).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("GET responde 200 con una lista vacía cuando todavía no hay decisiones")
    void listarShouldReturnEmptyWeeksWhenThereAreNoDecisions() throws Exception {
        when(listarDecisiones.execute(any())).thenReturn(new ListarDecisionesResponse(List.of()));

        ResultActions result = mockMvc.perform(get(DECISIONES).principal(principal));

        result.andExpect(status().isOk()).andExpect(jsonPath("$.semanas").isEmpty());
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void listarShouldReturn404WhenTheObjetivoIsNotFound() throws Exception {
        when(listarDecisiones.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        mockMvc.perform(get(DECISIONES).principal(principal)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void listarShouldReturn400WhenTheIdIsNotAUuid() throws Exception {
        ResultActions result = mockMvc.perform(get("/api/objetivos/no-es-un-uuid/decisiones").principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(listarDecisiones);
    }
}
