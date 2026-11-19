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
    private ResultActions respuesta;

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
    void postAnotaLaDecisionDelPasoYResponde201ConTodosSusDatos() throws Exception {
        dadoUnPasoQueSePuedeDecidir(TipoDecision.PASO_ACEPTADO);

        cuandoSeDecideElPaso("ACEPTADO");

        entoncesSeResponde201ConLaDecision();
    }

    @Test
    @DisplayName("POST manda al caso de uso el usuario de la sesión, el objetivo, el mini-objetivo y la respuesta")
    void postMandaAlCasoDeUsoElUsuarioDeLaSesionElObjetivoElMiniObjetivoYLaRespuesta() throws Exception {
        dadoUnPasoQueSePuedeDecidir(TipoDecision.PASO_IGNORADO);

        cuandoSeDecideElPaso("IGNORADO");

        entoncesElCasoDeUsoRecibeLaDecisionIgnorada();
    }

    @Test
    @DisplayName("POST responde 400 cuando falta la decisión y no llega al caso de uso")
    void postResponde400CuandoFaltaLaDecisionYNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeEnviaLaDecisionDelPaso("{}");

        entoncesSeRechazaPorqueFaltaLaDecision();
    }

    @Test
    @DisplayName("POST responde 400 cuando la decisión no es una de las permitidas")
    void postResponde400CuandoLaDecisionNoEsUnaDeLasPermitidas() throws Exception {
        cuandoSeDecideElPaso("AVISO_APLICADO");

        entoncesSeRechazaSinRegistrarLaDecision();
    }

    @Test
    @DisplayName("POST responde 404 cuando el mini-objetivo es de otro negocio o no existe")
    void postResponde404CuandoElMiniObjetivoEsDeOtroNegocioONoExiste() throws Exception {
        dadoUnMiniObjetivoInexistente();

        cuandoSeDecideElPaso("ACEPTADO");

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("POST responde 400 cuando el mini-objetivo ya no está en curso")
    void postResponde400CuandoElMiniObjetivoYaNoEstaEnCurso() throws Exception {
        dadoUnMiniObjetivoQueYaNoEstaEnCurso();

        cuandoSeDecideElPaso("ACEPTADO");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("GET lista las decisiones por semana con su efecto")
    void getListaLasDecisionesPorSemanaConSuEfecto() throws Exception {
        dadoUnaSemanaConUnAvisoAplicadoYMedido();

        cuandoSeListanLasDecisiones();

        entoncesSeListaLaSemanaConSuEfecto();
    }

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void getMandaAlCasoDeUsoElUsuarioDeLaSesionYElObjetivoDeLaRuta() throws Exception {
        dadoUnObjetivoSinDecisiones();

        cuandoSeListanLasDecisiones();

        entoncesElCasoDeUsoRecibeElUsuarioYElObjetivo();
    }

    @Test
    @DisplayName("GET responde 200 con una lista vacía cuando todavía no hay decisiones")
    void getResponde200ConUnaListaVaciaCuandoTodaviaNoHayDecisiones() throws Exception {
        dadoUnObjetivoSinDecisiones();

        cuandoSeListanLasDecisiones();

        entoncesSeDevuelvenSemanasVacias();
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void getResponde404CuandoElObjetivoEsDeOtroNegocioONoExiste() throws Exception {
        dadoUnObjetivoInexistente();

        cuandoSeListanLasDecisiones();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void getResponde400CuandoElIdDelObjetivoNoEsUnUuid() throws Exception {
        cuandoSeListanLasDecisionesDeUnObjetivoConIdInvalido();

        entoncesSeRechazaElListadoSinLlegarAlCasoDeUso();
    }

    // --- dado ---

    private void dadoUnPasoQueSePuedeDecidir(TipoDecision tipo) {
        when(registrarDecisionPaso.execute(any())).thenReturn(
                DecisionItem.de(decision(tipo, null), EfectoDecision.SIN_MEDICION));
    }

    private void dadoUnMiniObjetivoInexistente() {
        when(registrarDecisionPaso.execute(any()))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }

    private void dadoUnMiniObjetivoQueYaNoEstaEnCurso() {
        when(registrarDecisionPaso.execute(any()))
                .thenThrow(new BusinessRuleException("El mini-objetivo ya no está en curso"));
    }

    private void dadoUnaSemanaConUnAvisoAplicadoYMedido() {
        DecisionItem item = DecisionItem.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID),
                new EfectoDecision(EstadoEfecto.MEDIDO, new BigDecimal("350.00")));
        when(listarDecisiones.execute(any()))
                .thenReturn(new ListarDecisionesResponse(List.of(new SemanaDecisiones(SEMANA, List.of(item)))));
    }

    private void dadoUnObjetivoSinDecisiones() {
        when(listarDecisiones.execute(any())).thenReturn(new ListarDecisionesResponse(List.of()));
    }

    private void dadoUnObjetivoInexistente() {
        when(listarDecisiones.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeDecideElPaso(String tipo) throws Exception {
        cuandoSeEnviaLaDecisionDelPaso("{\"tipo\":\"" + tipo + "\"}");
    }

    private void cuandoSeEnviaLaDecisionDelPaso(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post(PASO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void cuandoSeListanLasDecisiones() throws Exception {
        respuesta = mockMvc.perform(get(DECISIONES).principal(principal));
    }

    private void cuandoSeListanLasDecisionesDeUnObjetivoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/objetivos/no-es-un-uuid/decisiones").principal(principal));
    }

    // --- entonces ---

    private void entoncesSeResponde201ConLaDecision() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(DECISION_ID.toString()))
                .andExpect(jsonPath("$.tipo").value("PASO_ACEPTADO"))
                .andExpect(jsonPath("$.miniObjetivoId").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.descripcion").value(TITULO_AVISO))
                .andExpect(jsonPath("$.semanaInicio").value("2026-11-09"))
                .andExpect(jsonPath("$.efecto").value("SIN_MEDICION"));
    }

    private void entoncesElCasoDeUsoRecibeLaDecisionIgnorada() throws Exception {
        respuesta.andExpect(status().isCreated());
        ArgumentCaptor<RegistrarDecisionPasoRequest> captor =
                ArgumentCaptor.forClass(RegistrarDecisionPasoRequest.class);
        verify(registrarDecisionPaso).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().decision()).isEqualTo(DecisionPaso.IGNORADO);
    }

    private void entoncesSeRechazaPorqueFaltaLaDecision() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").value("La decisión es obligatoria"));
        verifyNoInteractions(registrarDecisionPaso);
    }

    private void entoncesSeRechazaSinRegistrarLaDecision() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(registrarDecisionPaso);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesSeListaLaSemanaConSuEfecto() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].semanaInicio").value("2026-11-09"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("AVISO_APLICADO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].avisoId").value(AVISO_ID.toString()))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("MEDIDO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].deltaIngresoSemanal").value(350.0));
    }

    private void entoncesElCasoDeUsoRecibeElUsuarioYElObjetivo() throws Exception {
        respuesta.andExpect(status().isOk());
        ArgumentCaptor<ListarDecisionesRequest> captor = ArgumentCaptor.forClass(ListarDecisionesRequest.class);
        verify(listarDecisiones).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesSeDevuelvenSemanasVacias() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.semanas").isEmpty());
    }

    private void entoncesSeRechazaElListadoSinLlegarAlCasoDeUso() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(listarDecisiones);
    }
}
