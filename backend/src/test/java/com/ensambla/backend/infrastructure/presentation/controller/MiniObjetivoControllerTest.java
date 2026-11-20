package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.FaltanDatosMiniObjetivos;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatos;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatosResponse;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivosResponse;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivoRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.MiniObjetivoPresentationMapper;
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

import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MiniObjetivoControllerTest {

    private static final String BASE = "/api/objetivos/" + OBJETIVO_ID + "/mini-objetivos";
    private static final String CANDIDATOS = BASE + "/" + MINI_ID + "/candidatos";
    private static final String REEMPLAZO = BASE + "/" + MINI_ID + "/reemplazo";
    private static final String MINI_OBJETIVO = BASE + "/" + MINI_ID;

    private final ListarMiniObjetivos listarMiniObjetivos = mock(ListarMiniObjetivos.class);
    private final ListarCandidatos listarCandidatos = mock(ListarCandidatos.class);
    private final ReemplazarMiniObjetivo reemplazarMiniObjetivo = mock(ReemplazarMiniObjetivo.class);
    private final AbandonarMiniObjetivo abandonarMiniObjetivo = mock(AbandonarMiniObjetivo.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        MiniObjetivoController controller = new MiniObjetivoController(
                listarMiniObjetivos,
                listarCandidatos,
                reemplazarMiniObjetivo,
                abandonarMiniObjetivo,
                new MiniObjetivoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los mini-objetivos, su historia y lo que falta, con todos sus datos")
    void getListaLosMiniObjetivosSuHistoriaYLoQueFaltaConTodosSusDatos() throws Exception {
        dadoUnObjetivoConUnMiniObjetivoEnCurso();

        cuandoSeListanLosMiniObjetivos();

        entoncesSeListaElMiniObjetivoConTodosSusDatos();
    }

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void getMandaAlCasoDeUsoElUsuarioDeLaSesionYElObjetivoDeLaRuta() throws Exception {
        dadoUnObjetivoSinMiniObjetivos();

        cuandoSeListanLosMiniObjetivos();

        entoncesElCasoDeUsoRecibeElUsuarioYElObjetivo();
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void getResponde404CuandoElObjetivoEsDeOtroNegocioONoExiste() throws Exception {
        dadoUnObjetivoInexistente();

        cuandoSeListanLosMiniObjetivos();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void getResponde400CuandoElIdDelObjetivoNoEsUnUuid() throws Exception {
        cuandoSeListanLosMiniObjetivosDeUnObjetivoConIdInvalido();

        entoncesSeRechazaSinLlegarA(listarMiniObjetivos);
    }

    @Test
    @DisplayName("GET de candidatos devuelve las alternativas con todos sus datos")
    void getDeCandidatosDevuelveLasAlternativasConTodosSusDatos() throws Exception {
        dadoUnMiniObjetivoConUnaAlternativa();

        cuandoSeListanLosCandidatos();

        entoncesSeListaLaAlternativaConTodosSusDatos();
        entoncesElCasoDeUsoRecibeElMiniObjetivoDelUsuario();
    }

    @Test
    @DisplayName("GET de candidatos responde 400 cuando el mini-objetivo ya no está en curso")
    void getDeCandidatosResponde400CuandoElMiniObjetivoYaNoEstaEnCurso() throws Exception {
        dadoUnMiniObjetivoQueYaNoEstaEnCurso();

        cuandoSeListanLosCandidatos();

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("POST de reemplazo manda el candidato elegido al caso de uso y devuelve el nuevo mini-objetivo")
    void postDeReemplazoMandaElCandidatoElegidoAlCasoDeUsoYDevuelveElNuevoMiniObjetivo() throws Exception {
        dadoUnMiniObjetivoReemplazable();

        cuandoSeReemplazaElMiniObjetivoPorElCandidatoDeLaYerba();

        entoncesSeDevuelveElNuevoMiniObjetivo();
        entoncesElCasoDeUsoRecibeElCandidatoElegido();
    }

    @Test
    @DisplayName("POST de reemplazo acepta un candidato sin producto ni referencia")
    void postDeReemplazoAceptaUnCandidatoSinProductoNiReferencia() throws Exception {
        dadoUnMiniObjetivoReemplazable();

        cuandoSeReemplazaElMiniObjetivo("{\"tipo\":\"MEJORAR_MARGEN\"}");

        entoncesElCasoDeUsoRecibeUnCandidatoSinProductoNiReferencia();
    }

    @Test
    @DisplayName("POST de reemplazo responde 400 sin tipo y no llega al caso de uso")
    void postDeReemplazoResponde400SinTipoYNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeReemplazaElMiniObjetivo("{}");

        entoncesSeRechazaElReemplazoSinTipo();
    }

    @Test
    @DisplayName("POST de reemplazo responde 400 cuando el mini-objetivo ya no está en curso")
    void postDeReemplazoResponde400CuandoElMiniObjetivoElegidoYaNoEstaDisponible() throws Exception {
        dadoUnCandidatoQueYaNoEstaDisponible();

        cuandoSeReemplazaElMiniObjetivo("{\"tipo\":\"MEJORAR_MARGEN\"}");

        entoncesSeRechazaElPedido();
    }

    @Test
    @DisplayName("DELETE quita el mini-objetivo y responde 204")
    void deleteQuitaElMiniObjetivoYResponde204() throws Exception {
        cuandoSeAbandonaElMiniObjetivo();

        entoncesSeQuitaElMiniObjetivoDelUsuario();
    }

    @Test
    @DisplayName("DELETE responde 404 cuando el mini-objetivo es de otro negocio o no existe")
    void deleteResponde404CuandoElMiniObjetivoEsDeOtroNegocioONoExiste() throws Exception {
        dadoUnMiniObjetivoInexistenteAlAbandonar();

        cuandoSeAbandonaElMiniObjetivo();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("DELETE responde 400 cuando el id del mini-objetivo no es un UUID")
    void deleteResponde400CuandoElIdDelMiniObjetivoNoEsUnUuid() throws Exception {
        cuandoSeAbandonaUnMiniObjetivoConIdInvalido();

        entoncesSeRechazaSinLlegarA(abandonarMiniObjetivo);
    }

    // --- dado ---

    private void dadoUnObjetivoConUnMiniObjetivoEnCurso() {
        when(listarMiniObjetivos.execute(any())).thenReturn(new ListarMiniObjetivosResponse(
                List.of(MiniObjetivoItem.de(miniObjetivo())), List.of(), FaltanDatosMiniObjetivos.POCOS_DATOS));
    }

    private void dadoUnObjetivoSinMiniObjetivos() {
        when(listarMiniObjetivos.execute(any())).thenReturn(
                new ListarMiniObjetivosResponse(List.of(), List.of(), null));
    }

    private void dadoUnObjetivoInexistente() {
        when(listarMiniObjetivos.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    private void dadoUnMiniObjetivoConUnaAlternativa() {
        when(listarCandidatos.execute(any())).thenReturn(ListarCandidatosResponse.de(List.of(candidato())));
    }

    private void dadoUnMiniObjetivoQueYaNoEstaEnCurso() {
        when(listarCandidatos.execute(any()))
                .thenThrow(new BusinessRuleException("El mini-objetivo ya no está en curso"));
    }

    private void dadoUnMiniObjetivoReemplazable() {
        when(reemplazarMiniObjetivo.execute(any())).thenReturn(MiniObjetivoItem.de(miniObjetivo()));
    }

    private void dadoUnCandidatoQueYaNoEstaDisponible() {
        when(reemplazarMiniObjetivo.execute(any())).thenThrow(
                new BusinessRuleException("Ese mini-objetivo ya no está disponible: elegí otro de la lista"));
    }

    private void dadoUnMiniObjetivoInexistenteAlAbandonar() {
        doThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"))
                .when(abandonarMiniObjetivo).execute(any());
    }

    // --- cuando ---

    private void cuandoSeListanLosMiniObjetivos() throws Exception {
        respuesta = mockMvc.perform(get(BASE).principal(principal));
    }

    private void cuandoSeListanLosMiniObjetivosDeUnObjetivoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/objetivos/no-es-uuid/mini-objetivos").principal(principal));
    }

    private void cuandoSeListanLosCandidatos() throws Exception {
        respuesta = mockMvc.perform(get(CANDIDATOS).principal(principal));
    }

    private void cuandoSeReemplazaElMiniObjetivoPorElCandidatoDeLaYerba() throws Exception {
        cuandoSeReemplazaElMiniObjetivo("""
                {"tipo":"SUBIR_PRECIO","registroId":"%s","referencia":"%s"}"""
                .formatted(PRODUCTO_ID, PRODUCTO));
    }

    private void cuandoSeReemplazaElMiniObjetivo(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post(REEMPLAZO).principal(principal)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    private void cuandoSeAbandonaElMiniObjetivo() throws Exception {
        respuesta = mockMvc.perform(delete(MINI_OBJETIVO).principal(principal));
    }

    private void cuandoSeAbandonaUnMiniObjetivoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(delete(BASE + "/no-es-uuid").principal(principal));
    }

    // --- entonces ---

    private void entoncesSeListaElMiniObjetivoConTodosSusDatos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.minis[0].id").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.minis[0].posicion").value(1))
                .andExpect(jsonPath("$.minis[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$.minis[0].sentido").value("SUBE"))
                .andExpect(jsonPath("$.minis[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.minis[0].titulo").value(TITULO))
                .andExpect(jsonPath("$.minis[0].justificacion").value(JUSTIFICACION))
                .andExpect(jsonPath("$.minis[0].referencia").value(PRODUCTO))
                .andExpect(jsonPath("$.minis[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$.minis[0].meta").value(14183.0))
                .andExpect(jsonPath("$.minis[0].valorInicial").value(12600.0))
                .andExpect(jsonPath("$.minis[0].valorActual").value(12600.0))
                .andExpect(jsonPath("$.minis[0].avance").value(0))
                .andExpect(jsonPath("$.minis[0].aporteEstimado").value(6786.0))
                .andExpect(jsonPath("$.minis[0].semanaInicio").value("2026-11-09"))
                .andExpect(jsonPath("$.minis[0].textoConIa").value(false))
                .andExpect(jsonPath("$.historial").isEmpty())
                .andExpect(jsonPath("$.faltanDatos").value("POCOS_DATOS"));
    }

    private void entoncesElCasoDeUsoRecibeElUsuarioYElObjetivo() throws Exception {
        respuesta.andExpect(status().isOk());
        ArgumentCaptor<ListarMiniObjetivosRequest> captor = ArgumentCaptor.forClass(ListarMiniObjetivosRequest.class);
        verify(listarMiniObjetivos).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesSeRechazaElPedido() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesSeRechazaSinLlegarA(Object casoDeUso) throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(casoDeUso);
    }

    private void entoncesSeListaLaAlternativaConTodosSusDatos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$[0].sentido").value("SUBE"))
                .andExpect(jsonPath("$[0].titulo").value(TITULO))
                .andExpect(jsonPath("$[0].referencia").value(PRODUCTO))
                .andExpect(jsonPath("$[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$[0].meta").value(14183.0))
                .andExpect(jsonPath("$[0].aporteEstimado").value(6786.0));
    }

    private void entoncesElCasoDeUsoRecibeElMiniObjetivoDelUsuario() {
        ArgumentCaptor<ListarCandidatosRequest> captor = ArgumentCaptor.forClass(ListarCandidatosRequest.class);
        verify(listarCandidatos).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    private void entoncesSeDevuelveElNuevoMiniObjetivo() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.tipo").value("SUBIR_PRECIO"));
    }

    private void entoncesElCasoDeUsoRecibeElCandidatoElegido() {
        ReemplazarMiniObjetivoRequest pedido = pedidoDeReemplazo();
        assertThat(pedido.usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(pedido.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(pedido.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(pedido.tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(pedido.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(pedido.referencia()).isEqualTo(PRODUCTO);
    }

    private void entoncesElCasoDeUsoRecibeUnCandidatoSinProductoNiReferencia() throws Exception {
        respuesta.andExpect(status().isOk());
        assertThat(pedidoDeReemplazo().registroId()).isNull();
        assertThat(pedidoDeReemplazo().referencia()).isNull();
    }

    private void entoncesSeRechazaElReemplazoSinTipo() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").value("El tipo es obligatorio"));
        verifyNoInteractions(reemplazarMiniObjetivo);
    }

    private void entoncesSeQuitaElMiniObjetivoDelUsuario() throws Exception {
        respuesta.andExpect(status().isNoContent());
        ArgumentCaptor<AbandonarMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(AbandonarMiniObjetivoRequest.class);
        verify(abandonarMiniObjetivo).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    // --- datos ---

    private ReemplazarMiniObjetivoRequest pedidoDeReemplazo() {
        ArgumentCaptor<ReemplazarMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(ReemplazarMiniObjetivoRequest.class);
        verify(reemplazarMiniObjetivo).execute(captor.capture());
        return captor.getValue();
    }
}
