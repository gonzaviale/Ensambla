package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.sami.ConversarConSamiRequest;
import com.ensambla.backend.domain.dto.sami.ConversarConSamiResponse;
import com.ensambla.backend.domain.dto.sami.CrearProyectoDesdeSamiRequest;
import com.ensambla.backend.domain.dto.sami.OpcionSamiItem;
import com.ensambla.backend.domain.dto.sami.PreguntaSamiItem;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.useCase.sami.ConversarConSamiUseCase;
import com.ensambla.backend.domain.useCase.sami.CrearProyectoDesdeSamiUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.proyecto.ProyectoPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.mapper.sami.SamiPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SamiControllerTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final UUID PLANTILLA_ID = UUID.randomUUID();
    private static final UUID PROYECTO_ID = UUID.randomUUID();

    private ConversarConSamiUseCase conversarConSamiUseCase;
    private CrearProyectoDesdeSamiUseCase crearProyectoDesdeSamiUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        conversarConSamiUseCase = mock(ConversarConSamiUseCase.class);
        crearProyectoDesdeSamiUseCase = mock(CrearProyectoDesdeSamiUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SamiController(
                        conversarConSamiUseCase,
                        crearProyectoDesdeSamiUseCase,
                        new SamiPresentationMapper(),
                        new ProyectoPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    @DisplayName("Devuelve la pregunta de Sami y le pasa el usuario de la sesión, nunca uno del body")
    void conversarShouldReturnQuestionForSessionUser() throws Exception {
        when(conversarConSamiUseCase.execute(any())).thenReturn(new ConversarConSamiResponse("PREGUNTA",
                new PreguntaSamiItem("principal", "¿Qué querés anotar?", List.of(new OpcionSamiItem("ventas", "Vender"))),
                null, 0));

        ResultActions result = whenPosting("/api/sami/conversacion",
                "{\"texto\":\"quiero ordenar mi negocio\",\"usuarioId\":\"otro\"}");

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("PREGUNTA"))
                .andExpect(jsonPath("$.pregunta.opciones[0].id").value("ventas"))
                .andExpect(jsonPath("$.propuesta").doesNotExist());
        ArgumentCaptor<ConversarConSamiRequest> captor = ArgumentCaptor.forClass(ConversarConSamiRequest.class);
        verify(conversarConSamiUseCase).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Rechaza con 400 un texto vacío")
    void conversarShouldRejectBlankText() throws Exception {
        ResultActions result = whenPosting("/api/sami/conversacion", "{\"texto\":\"   \"}");

        result.andExpect(status().isBadRequest());
        verify(conversarConSamiUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Rechaza con 400 una respuesta sin opción")
    void conversarShouldRejectAnswerWithoutOption() throws Exception {
        ResultActions result = whenPosting("/api/sami/conversacion",
                "{\"texto\":\"hola\",\"respuestas\":[{\"preguntaId\":\"principal\"}]}");

        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Crea el proyecto con las columnas confirmadas, responde 201 con su ubicación y usa la sesión")
    void crearProyectoShouldReturnCreatedProject() throws Exception {
        when(crearProyectoDesdeSamiUseCase.execute(any())).thenReturn(proyecto());

        ResultActions result = whenPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","nombre":"Velas","columnas":[
                  {"nombre":"Saldo pendiente","tipo":"NUMERO"}]}
                """.formatted(PLANTILLA_ID));

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/proyectos/" + PROYECTO_ID))
                .andExpect(jsonPath("$.id").value(PROYECTO_ID.toString()))
                .andExpect(jsonPath("$.nombre").value("Velas"));
        ArgumentCaptor<CrearProyectoDesdeSamiRequest> captor =
                ArgumentCaptor.forClass(CrearProyectoDesdeSamiRequest.class);
        verify(crearProyectoDesdeSamiUseCase).execute(captor.capture());
        assertThat(captor.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(captor.getValue().plantillaId()).isEqualTo(PLANTILLA_ID);
        assertThat(captor.getValue().columnas()).extracting("nombre", "tipo")
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Saldo pendiente", "NUMERO"));
    }

    @Test
    @DisplayName("Rechaza con 400 un pedido sin plantilla o sin lista de columnas")
    void crearProyectoShouldRejectMissingTemplateOrColumns() throws Exception {
        whenPosting("/api/sami/proyectos", "{\"columnas\":[]}").andExpect(status().isBadRequest());
        whenPosting("/api/sami/proyectos", "{\"plantillaId\":\"%s\"}".formatted(PLANTILLA_ID))
                .andExpect(status().isBadRequest());
        verify(crearProyectoDesdeSamiUseCase, never()).execute(any());
    }

    @Test
    @DisplayName("Rechaza con 400 una columna sin nombre o sin tipo")
    void crearProyectoShouldRejectColumnWithoutNameOrType() throws Exception {
        whenPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"","tipo":"TEXTO"}]}
                """.formatted(PLANTILLA_ID)).andExpect(status().isBadRequest());
        whenPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"Saldo"}]}
                """.formatted(PLANTILLA_ID)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Si una columna no es válida responde 400 con el error ubicado en la columna")
    void crearProyectoShouldReturnLocatedColumnErrors() throws Exception {
        when(crearProyectoDesdeSamiUseCase.execute(any())).thenThrow(
                new CampoInvalidoException(Map.of("columnas[0].nombre", "Escribí un nombre")));

        ResultActions result = whenPosting("/api/sami/proyectos", """
                {"plantillaId":"%s","columnas":[{"nombre":"x","tipo":"TEXTO"}]}
                """.formatted(PLANTILLA_ID));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['columnas[0].nombre']").value("Escribí un nombre"));
    }

    private ProyectoItem proyecto() {
        return new ProyectoItem(PROYECTO_ID, "Velas", null, PLANTILLA_ID,
                OffsetDateTime.parse("2026-10-19T12:00:00Z"), OffsetDateTime.parse("2026-10-19T12:00:00Z"), List.of());
    }

    private ResultActions whenPosting(String url, String json) throws Exception {
        return mockMvc.perform(post(url)
                .principal(new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), "n/a"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }
}
