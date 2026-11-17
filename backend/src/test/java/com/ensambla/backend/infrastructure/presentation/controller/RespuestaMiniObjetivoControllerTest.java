package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientes;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientesResponse;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.MiniObjetivoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.RespuestaMiniObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO_MANUAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniManual;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.COMENTARIO;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.RESPUESTA_ID;
import static com.ensambla.backend.support.RespuestaMiniObjetivoTestData.respuesta;
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

class RespuestaMiniObjetivoControllerTest {

    private static final String RESPUESTA = "/api/objetivos/" + OBJETIVO_ID + "/mini-objetivos/" + OTRO_MINI_ID
            + "/respuesta";
    private static final String RESPUESTA_CON_ID_INVALIDO = "/api/objetivos/" + OBJETIVO_ID
            + "/mini-objetivos/xyz/respuesta";
    private static final String PENDIENTES = "/api/objetivos/respuestas-pendientes";
    private static final String CUERPO = "{\"hizo\":\"EN_PARTE\",\"quePaso\":\"NO_TUVE_TIEMPO\","
            + "\"comentario\":\"" + COMENTARIO + "\"}";

    private final ResponderMiniObjetivo responderMiniObjetivo = mock(ResponderMiniObjetivo.class);
    private final ListarRespuestasPendientes listarRespuestasPendientes = mock(ListarRespuestasPendientes.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;
    private ResultActions resultado;

    @BeforeEach
    void setUp() {
        RespuestaMiniObjetivoController controller = new RespuestaMiniObjetivoController(
                responderMiniObjetivo, listarRespuestasPendientes,
                new RespuestaMiniObjetivoPresentationMapper(new MiniObjetivoPresentationMapper()));
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST guarda la respuesta y responde 201 con todos sus datos")
    void responderShouldReturnCreatedWithTheAnswer() throws Exception {
        givenSeGuardaLaRespuesta();

        whenResponder(CUERPO);

        thenSeResponde201ConLaRespuesta();
    }

    @Test
    @DisplayName("POST manda al caso de uso el usuario de la sesión, los ids de la ruta y la respuesta")
    void responderShouldPassTheRequestToTheUseCase() throws Exception {
        givenSeGuardaLaRespuesta();

        whenResponder(CUERPO);

        thenElCasoDeUsoRecibeUsuarioIdsYRespuesta();
    }

    @Test
    @DisplayName("POST responde 400 cuando falta si lo hizo y no llega al caso de uso")
    void responderShouldReturn400WhenHizoIsMissing() throws Exception {
        whenResponder("{\"quePaso\":\"FUNCIONO\"}");

        thenSeResponde400ConElError("hizo", "Contá si lo hiciste");
        thenNoLlegaAlCasoDeUso();
    }

    @Test
    @DisplayName("POST responde 400 cuando falta qué pasó y no llega al caso de uso")
    void responderShouldReturn400WhenQuePasoIsMissing() throws Exception {
        whenResponder("{\"hizo\":\"SI\"}");

        thenSeResponde400ConElError("quePaso", "Elegí qué pasó");
        thenNoLlegaAlCasoDeUso();
    }

    @Test
    @DisplayName("POST responde 400 cuando el comentario pasa los 300 caracteres")
    void responderShouldReturn400WhenTheCommentIsTooLong() throws Exception {
        whenResponder("{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\",\"comentario\":\"" + "a".repeat(301) + "\"}");

        thenSeResponde400ConElError("comentario", "El comentario no puede tener más de 300 caracteres");
        thenNoLlegaAlCasoDeUso();
    }

    @Test
    @DisplayName("POST responde 400 cuando la respuesta no es una de las permitidas")
    void responderShouldReturn400WhenTheAnswerIsUnknown() throws Exception {
        whenResponder("{\"hizo\":\"TAL_VEZ\",\"quePaso\":\"FUNCIONO\"}");

        thenSeResponde400();
        thenNoLlegaAlCasoDeUso();
    }

    @Test
    @DisplayName("POST responde 400 cuando la combinación es contradictoria")
    void responderShouldReturn400WhenTheCombinationIsContradictory() throws Exception {
        givenUnaCombinacionContradictoria();

        whenResponder("{\"hizo\":\"SI\",\"quePaso\":\"NO_TUVE_TIEMPO\"}");

        thenSeResponde400();
    }

    @Test
    @DisplayName("POST responde 409 cuando el mini-objetivo ya se respondió")
    void responderShouldReturn409WhenAlreadyAnswered() throws Exception {
        givenUnMiniYaRespondido();

        whenResponder(CUERPO);

        thenSeResponde409();
    }

    @Test
    @DisplayName("POST responde 404 cuando el mini-objetivo es de otro negocio o no existe")
    void responderShouldReturn404WhenTheMiniObjetivoIsNotFound() throws Exception {
        givenUnMiniInexistente();

        whenResponder(CUERPO);

        thenSeResponde404();
    }

    @Test
    @DisplayName("POST responde 400 cuando un id de la ruta no es un UUID")
    void responderShouldReturn400WhenTheIdIsNotUuid() throws Exception {
        whenResponderEn(RESPUESTA_CON_ID_INVALIDO, CUERPO);

        thenSeResponde400();
        thenNoLlegaAlCasoDeUso();
    }

    @Test
    @DisplayName("GET lista los manuales pendientes con el objetivo al que pertenecen")
    void pendientesShouldReturnThePendingOnes() throws Exception {
        givenUnManualPendiente();

        whenSePidenLosPendientes();

        thenSeListaElManualPendienteConSuObjetivo();
        thenSeConsultaConElUsuarioDeLaSesion();
    }

    @Test
    @DisplayName("GET sin pendientes responde una lista vacía")
    void pendientesShouldReturnAnEmptyList() throws Exception {
        givenNingunManualPendiente();

        whenSePidenLosPendientes();

        thenSeResponde200ConUnaListaVacia();
    }

    // --- arrange ---
    private void givenSeGuardaLaRespuesta() {
        when(responderMiniObjetivo.execute(any())).thenReturn(ResponderMiniObjetivoResponse.de(respuesta()));
    }

    private void givenUnaCombinacionContradictoria() {
        when(responderMiniObjetivo.execute(any())).thenThrow(
                new BusinessRuleException("Si lo hiciste, no puede ser que no hayas tenido tiempo"));
    }

    private void givenUnMiniYaRespondido() {
        when(responderMiniObjetivo.execute(any()))
                .thenThrow(new DuplicateResourceException("Ya respondiste este mini-objetivo"));
    }

    private void givenUnMiniInexistente() {
        when(responderMiniObjetivo.execute(any()))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }

    private void givenUnManualPendiente() {
        when(listarRespuestasPendientes.execute(any())).thenReturn(
                ListarRespuestasPendientesResponse.de(List.of(objetivo()), List.of(miniManual())));
    }

    private void givenNingunManualPendiente() {
        when(listarRespuestasPendientes.execute(any()))
                .thenReturn(ListarRespuestasPendientesResponse.de(List.of(), List.of()));
    }

    // --- act ---
    private void whenResponder(String cuerpo) throws Exception {
        whenResponderEn(RESPUESTA, cuerpo);
    }

    private void whenResponderEn(String ruta, String cuerpo) throws Exception {
        resultado = mockMvc.perform(post(ruta).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void whenSePidenLosPendientes() throws Exception {
        resultado = mockMvc.perform(get(PENDIENTES).principal(principal));
    }

    // --- assert ---
    private void thenSeResponde201ConLaRespuesta() throws Exception {
        resultado.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RESPUESTA_ID.toString()))
                .andExpect(jsonPath("$.miniObjetivoId").value(OTRO_MINI_ID.toString()))
                .andExpect(jsonPath("$.hizo").value("EN_PARTE"))
                .andExpect(jsonPath("$.quePaso").value("NO_TUVE_TIEMPO"))
                .andExpect(jsonPath("$.comentario").value(COMENTARIO))
                .andExpect(jsonPath("$.semanaInicio").value("2026-11-09"));
    }

    private void thenElCasoDeUsoRecibeUsuarioIdsYRespuesta() throws Exception {
        resultado.andExpect(status().isCreated());
        ArgumentCaptor<ResponderMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(ResponderMiniObjetivoRequest.class);
        verify(responderMiniObjetivo).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(OTRO_MINI_ID);
        assertThat(captor.getValue().hizo()).isEqualTo(HizoManual.EN_PARTE);
        assertThat(captor.getValue().quePaso()).isEqualTo(QuePaso.NO_TUVE_TIEMPO);
        assertThat(captor.getValue().comentario()).isEqualTo(COMENTARIO);
    }

    private void thenSeResponde400() throws Exception {
        resultado.andExpect(status().isBadRequest());
    }

    private void thenSeResponde400ConElError(String campo, String mensaje) throws Exception {
        resultado.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + campo).value(mensaje));
    }

    private void thenSeResponde409() throws Exception {
        resultado.andExpect(status().isConflict());
    }

    private void thenSeResponde404() throws Exception {
        resultado.andExpect(status().isNotFound());
    }

    private void thenNoLlegaAlCasoDeUso() {
        verifyNoInteractions(responderMiniObjetivo);
    }

    private void thenSeListaElManualPendienteConSuObjetivo() throws Exception {
        resultado.andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objetivoId").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$[0].objetivoTexto").value(TEXTO))
                .andExpect(jsonPath("$[0].miniObjetivo.id").value(OTRO_MINI_ID.toString()))
                .andExpect(jsonPath("$[0].miniObjetivo.modo").value("MANUAL"))
                .andExpect(jsonPath("$[0].miniObjetivo.titulo").value(TITULO_MANUAL))
                .andExpect(jsonPath("$[0].miniObjetivo.tipo").doesNotExist())
                .andExpect(jsonPath("$[0].miniObjetivo.meta").doesNotExist());
    }

    private void thenSeConsultaConElUsuarioDeLaSesion() {
        ArgumentCaptor<ListarRespuestasPendientesRequest> captor =
                ArgumentCaptor.forClass(ListarRespuestasPendientesRequest.class);
        verify(listarRespuestasPendientes).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
    }

    private void thenSeResponde200ConUnaListaVacia() throws Exception {
        resultado.andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
}
