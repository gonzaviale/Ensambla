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

    private final ListarMiniObjetivos listarMiniObjetivos = mock(ListarMiniObjetivos.class);
    private final ListarCandidatos listarCandidatos = mock(ListarCandidatos.class);
    private final ReemplazarMiniObjetivo reemplazarMiniObjetivo = mock(ReemplazarMiniObjetivo.class);
    private final AbandonarMiniObjetivo abandonarMiniObjetivo = mock(AbandonarMiniObjetivo.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

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
    void listarShouldReturnMappedMiniObjetivos() throws Exception {
        when(listarMiniObjetivos.execute(any())).thenReturn(new ListarMiniObjetivosResponse(
                List.of(MiniObjetivoItem.de(miniObjetivo())), List.of(), FaltanDatosMiniObjetivos.POCOS_DATOS));

        ResultActions result = mockMvc.perform(get(BASE).principal(principal));

        result.andExpect(status().isOk())
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

    @Test
    @DisplayName("GET manda al caso de uso el usuario de la sesión y el objetivo de la ruta")
    void listarShouldPassTheSessionUserAndTheObjetivo() throws Exception {
        when(listarMiniObjetivos.execute(any())).thenReturn(
                new ListarMiniObjetivosResponse(List.of(), List.of(), null));

        mockMvc.perform(get(BASE).principal(principal)).andExpect(status().isOk());

        ArgumentCaptor<ListarMiniObjetivosRequest> captor = ArgumentCaptor.forClass(ListarMiniObjetivosRequest.class);
        verify(listarMiniObjetivos).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("GET responde 404 cuando el objetivo es de otro negocio o no existe")
    void listarShouldReturn404WhenTheObjetivoIsNotFound() throws Exception {
        when(listarMiniObjetivos.execute(any())).thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        mockMvc.perform(get(BASE).principal(principal)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET responde 400 cuando el id del objetivo no es un UUID")
    void listarShouldReturn400WhenTheObjetivoIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/objetivos/no-es-uuid/mini-objetivos").principal(principal))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listarMiniObjetivos);
    }

    @Test
    @DisplayName("GET de candidatos devuelve las alternativas con todos sus datos")
    void candidatosShouldReturnMappedAlternatives() throws Exception {
        when(listarCandidatos.execute(any())).thenReturn(ListarCandidatosResponse.de(List.of(candidato())));

        ResultActions result = mockMvc.perform(get(BASE + "/" + MINI_ID + "/candidatos").principal(principal));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("SUBIR_PRECIO"))
                .andExpect(jsonPath("$[0].sentido").value("SUBE"))
                .andExpect(jsonPath("$[0].titulo").value(TITULO))
                .andExpect(jsonPath("$[0].referencia").value(PRODUCTO))
                .andExpect(jsonPath("$[0].registroId").value(PRODUCTO_ID.toString()))
                .andExpect(jsonPath("$[0].meta").value(14183.0))
                .andExpect(jsonPath("$[0].aporteEstimado").value(6786.0));
        ArgumentCaptor<ListarCandidatosRequest> captor = ArgumentCaptor.forClass(ListarCandidatosRequest.class);
        verify(listarCandidatos).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("GET de candidatos responde 400 cuando el mini-objetivo ya no está en curso")
    void candidatosShouldReturn400WhenTheMiniObjetivoIsNotInProgress() throws Exception {
        when(listarCandidatos.execute(any()))
                .thenThrow(new BusinessRuleException("El mini-objetivo ya no está en curso"));

        mockMvc.perform(get(BASE + "/" + MINI_ID + "/candidatos").principal(principal))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST de reemplazo manda el candidato elegido al caso de uso y devuelve el nuevo mini-objetivo")
    void reemplazarShouldPassTheChosenCandidateAndReturnTheNewMiniObjetivo() throws Exception {
        when(reemplazarMiniObjetivo.execute(any())).thenReturn(MiniObjetivoItem.de(miniObjetivo()));

        ResultActions result = mockMvc.perform(post(BASE + "/" + MINI_ID + "/reemplazo")
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"tipo":"SUBIR_PRECIO","registroId":"%s","referencia":"%s"}"""
                        .formatted(PRODUCTO_ID, PRODUCTO)));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MINI_ID.toString()))
                .andExpect(jsonPath("$.tipo").value("SUBIR_PRECIO"));
        ArgumentCaptor<ReemplazarMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(ReemplazarMiniObjetivoRequest.class);
        verify(reemplazarMiniObjetivo).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().tipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(captor.getValue().registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(captor.getValue().referencia()).isEqualTo(PRODUCTO);
    }

    @Test
    @DisplayName("POST de reemplazo acepta un candidato sin producto ni referencia")
    void reemplazarShouldAcceptACandidateWithoutProduct() throws Exception {
        when(reemplazarMiniObjetivo.execute(any())).thenReturn(MiniObjetivoItem.de(miniObjetivo()));

        mockMvc.perform(post(BASE + "/" + MINI_ID + "/reemplazo")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MEJORAR_MARGEN\"}"))
                .andExpect(status().isOk());

        ArgumentCaptor<ReemplazarMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(ReemplazarMiniObjetivoRequest.class);
        verify(reemplazarMiniObjetivo).execute(captor.capture());
        assertThat(captor.getValue().registroId()).isNull();
        assertThat(captor.getValue().referencia()).isNull();
    }

    @Test
    @DisplayName("POST de reemplazo responde 400 sin tipo y no llega al caso de uso")
    void reemplazarShouldReturn400WhenTheTypeIsMissing() throws Exception {
        mockMvc.perform(post(BASE + "/" + MINI_ID + "/reemplazo")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.tipo").value("El tipo es obligatorio"));

        verifyNoInteractions(reemplazarMiniObjetivo);
    }

    @Test
    @DisplayName("POST de reemplazo responde 400 cuando el mini-objetivo ya no está en curso")
    void reemplazarShouldReturn400WhenTheChosenOneIsNotAvailable() throws Exception {
        when(reemplazarMiniObjetivo.execute(any())).thenThrow(
                new BusinessRuleException("Ese mini-objetivo ya no está disponible: elegí otro de la lista"));

        mockMvc.perform(post(BASE + "/" + MINI_ID + "/reemplazo")
                        .principal(principal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"MEJORAR_MARGEN\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("DELETE quita el mini-objetivo y responde 204")
    void abandonarShouldReturn204() throws Exception {
        mockMvc.perform(delete(BASE + "/" + MINI_ID).principal(principal)).andExpect(status().isNoContent());

        ArgumentCaptor<AbandonarMiniObjetivoRequest> captor =
                ArgumentCaptor.forClass(AbandonarMiniObjetivoRequest.class);
        verify(abandonarMiniObjetivo).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(captor.getValue().miniObjetivoId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("DELETE responde 404 cuando el mini-objetivo es de otro negocio o no existe")
    void abandonarShouldReturn404WhenTheMiniObjetivoIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"))
                .when(abandonarMiniObjetivo).execute(any());

        mockMvc.perform(delete(BASE + "/" + MINI_ID).principal(principal)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE responde 400 cuando el id del mini-objetivo no es un UUID")
    void abandonarShouldReturn400WhenTheMiniObjetivoIdIsNotAUuid() throws Exception {
        mockMvc.perform(delete(BASE + "/no-es-uuid").principal(principal)).andExpect(status().isBadRequest());

        verifyNoInteractions(abandonarMiniObjetivo);
    }
}
