package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivoRequest;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivosRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.META;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.TEXTO;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObjetivoControllerTest {

    private final ListarObjetivos listarObjetivos = mock(ListarObjetivos.class);
    private final CrearObjetivo crearObjetivo = mock(CrearObjetivo.class);
    private final EliminarObjetivo eliminarObjetivo = mock(EliminarObjetivo.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjetivoController controller = new ObjetivoController(
                listarObjetivos,
                crearObjetivo,
                eliminarObjetivo,
                new ObjetivoPresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los objetivos y mapea todos sus datos a JSON")
    void listarShouldReturnMappedObjetivos() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of(givenItem()));

        ResultActions result = whenGetListar();

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$[0].texto").value(TEXTO))
                .andExpect(jsonPath("$[0].meta").value(META.doubleValue()))
                .andExpect(jsonPath("$[0].creadoEn").exists());
    }

    @Test
    @DisplayName("GET delega en el caso de uso con el usuario de la sesión")
    void listarShouldPassSessionUsuario() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of());

        whenGetListar();

        verify(listarObjetivos).execute(new ListarObjetivosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando no hay objetivos")
    void listarShouldReturnEmptyList() throws Exception {
        when(listarObjetivos.execute(any())).thenReturn(List.of());

        ResultActions result = whenGetListar();

        result.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("POST crea el objetivo y devuelve 201 con su ubicación")
    void crearShouldReturn201WithLocation() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(givenItem());

        ResultActions result = whenPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50}");

        result.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/objetivos/" + OBJETIVO_ID))
                .andExpect(jsonPath("$.id").value(OBJETIVO_ID.toString()))
                .andExpect(jsonPath("$.texto").value(TEXTO));
    }

    @Test
    @DisplayName("POST pasa el texto y la meta al caso de uso con el usuario de la sesión")
    void crearShouldPassRequestToUseCase() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(givenItem());

        whenPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50}");

        verify(crearObjetivo).execute(new CrearObjetivoRequest(USUARIO_ID, TEXTO, new BigDecimal("400000.50")));
    }

    @Test
    @DisplayName("POST ignora el negocio que indique el navegador")
    void crearShouldIgnoreClientNegocio() throws Exception {
        when(crearObjetivo.execute(any())).thenReturn(givenItem());

        ResultActions result = whenPostCrear(
                "{\"texto\":\"" + TEXTO + "\",\"meta\":400000.50,\"negocioId\":\"otro\"}");

        result.andExpect(status().isCreated());
        verify(crearObjetivo).execute(new CrearObjetivoRequest(USUARIO_ID, TEXTO, new BigDecimal("400000.50")));
    }

    @Test
    @DisplayName("POST rechaza un texto vacío antes de invocar el caso de uso")
    void crearShouldRejectBlankText() throws Exception {
        ResultActions result = whenPostCrear("{\"texto\":\"  \",\"meta\":400000}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.texto").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza un texto de más de 500 caracteres")
    void crearShouldRejectTooLongText() throws Exception {
        ResultActions result = whenPostCrear("{\"texto\":\"" + "a".repeat(501) + "\",\"meta\":400000}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.texto").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza un body sin meta")
    void crearShouldRejectMissingMeta() throws Exception {
        ResultActions result = whenPostCrear("{\"texto\":\"" + TEXTO + "\"}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.meta").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("POST rechaza una meta menor a 1")
    void crearShouldRejectMetaBelowOne() throws Exception {
        ResultActions result = whenPostCrear("{\"texto\":\"" + TEXTO + "\",\"meta\":0}");

        result.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.meta").exists());
        verifyNoInteractions(crearObjetivo);
    }

    @Test
    @DisplayName("DELETE devuelve 204 y delega con el usuario de la sesión y el id de la ruta")
    void eliminarShouldReturn204() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/objetivos/{id}", OBJETIVO_ID).principal(principal));

        result.andExpect(status().isNoContent());
        verify(eliminarObjetivo).execute(new EliminarObjetivoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    @Test
    @DisplayName("DELETE de un objetivo ajeno o inexistente devuelve el 404 estándar")
    void eliminarShouldReturn404WhenNotOwned() throws Exception {
        doThrow(new ResourceNotFoundException("Objetivo no encontrado")).when(eliminarObjetivo).execute(any());

        ResultActions result = mockMvc.perform(delete("/api/objetivos/{id}", OBJETIVO_ID).principal(principal));

        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("DELETE con un id que no es UUID devuelve 400 sin llegar al caso de uso")
    void eliminarShouldReturn400WhenIdIsNotUuid() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/objetivos/invalido").principal(principal));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(eliminarObjetivo);
    }

    // --- arrange ---
    private ObjetivoItem givenItem() {
        return ObjetivoItem.de(objetivo());
    }

    // --- act ---
    private ResultActions whenGetListar() throws Exception {
        return mockMvc.perform(get("/api/objetivos").principal(principal));
    }

    private ResultActions whenPostCrear(String body) throws Exception {
        return mockMvc.perform(post("/api/objetivos").principal(principal)
                .contentType("application/json")
                .content(body));
    }
}
