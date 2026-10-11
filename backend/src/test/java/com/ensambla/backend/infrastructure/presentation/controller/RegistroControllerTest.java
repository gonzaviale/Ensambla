package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.registro.TotalesItem;
import java.math.BigDecimal;
import com.ensambla.backend.domain.dto.lote.FotoArchivoResponse;
import com.ensambla.backend.domain.dto.registro.CrearRegistroRequest;
import com.ensambla.backend.domain.dto.registro.CrearRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EditarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.EditarRegistroResponse;
import com.ensambla.backend.domain.dto.registro.EliminarRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosRequest;
import com.ensambla.backend.domain.dto.registro.ListarRegistrosResponse;
import com.ensambla.backend.domain.dto.registro.ObtenerFotoOrigenRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroRequest;
import com.ensambla.backend.domain.dto.registro.ObtenerRegistroResponse;
import com.ensambla.backend.domain.dto.registro.RegistroItem;
import com.ensambla.backend.domain.exception.RegistroInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.registro.CrearRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EditarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.EliminarRegistroUseCase;
import com.ensambla.backend.domain.useCase.registro.ListarRegistrosUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerFotoOrigenUseCase;
import com.ensambla.backend.domain.useCase.registro.ObtenerRegistroUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.registro.RegistroPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RegistroControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID REGISTRO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final RegistroItem ITEM = new RegistroItem(
            REGISTRO_ID, BLOQUE_ID, Map.of("campo", "dato"), null, AHORA, AHORA);
    private static final UUID CAMPO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final String CUERPO = "{\"valores\":{\"campo\":\"dato\"}}";

    private final Principal sesion = new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private CrearRegistroUseCase crearRegistroUseCase;
    private ListarRegistrosUseCase listarRegistrosUseCase;
    private ObtenerRegistroUseCase obtenerRegistroUseCase;
    private EditarRegistroUseCase editarRegistroUseCase;
    private EliminarRegistroUseCase eliminarRegistroUseCase;
    private ObtenerFotoOrigenUseCase obtenerFotoOrigenUseCase;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        crearRegistroUseCase = mock(CrearRegistroUseCase.class);
        listarRegistrosUseCase = mock(ListarRegistrosUseCase.class);
        obtenerRegistroUseCase = mock(ObtenerRegistroUseCase.class);
        editarRegistroUseCase = mock(EditarRegistroUseCase.class);
        eliminarRegistroUseCase = mock(EliminarRegistroUseCase.class);
        obtenerFotoOrigenUseCase = mock(ObtenerFotoOrigenUseCase.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new RegistroController(
                        crearRegistroUseCase,
                        listarRegistrosUseCase,
                        obtenerRegistroUseCase,
                        editarRegistroUseCase,
                        eliminarRegistroUseCase,
                        obtenerFotoOrigenUseCase,
                        new RegistroPresentationMapper()))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST crea el registro con el usuario de la sesión y responde 201")
    void crearShouldReturn201AndPassSessionUserToUseCase() throws Exception {
        when(crearRegistroUseCase.execute(any(CrearRegistroRequest.class)))
                .thenReturn(new CrearRegistroResponse(ITEM));

        ResultActions result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$.bloqueId").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$.valores.campo").value("dato"));
        ArgumentCaptor<CrearRegistroRequest> request = ArgumentCaptor.forClass(CrearRegistroRequest.class);
        verify(crearRegistroUseCase).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(request.getValue().valores()).containsEntry("campo", "dato");
    }

    @Test
    @DisplayName("GET de la foto de origen devuelve sus bytes y pasa el usuario de la sesión")
    void fotoOrigenShouldReturnBytesAndPassSessionUser() throws Exception {
        byte[] bytes = {1, 2, 3};
        when(obtenerFotoOrigenUseCase.execute(any(ObtenerFotoOrigenRequest.class)))
                .thenReturn(new FotoArchivoResponse(bytes, "image/png"));

        ResultActions result = mockMvc.perform(get("/api/registros/{id}/foto-origen", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(bytes));
        verify(obtenerFotoOrigenUseCase).execute(new ObtenerFotoOrigenRequest(USUARIO_ID, REGISTRO_ID));
    }

    @Test
    @DisplayName("GET de la foto de origen devuelve 404 cuando el registro no tiene foto")
    void fotoOrigenShouldReturn404WhenRegistroHasNoFoto() throws Exception {
        when(obtenerFotoOrigenUseCase.execute(any(ObtenerFotoOrigenRequest.class)))
                .thenThrow(new ResourceNotFoundException("El registro no tiene foto de origen"));

        ResultActions result = mockMvc.perform(get("/api/registros/{id}/foto-origen", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST responde 400 con el error de cada campo cuando los valores no son válidos")
    void crearShouldReturn400WithErrorsPerField() throws Exception {
        when(crearRegistroUseCase.execute(any(CrearRegistroRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("campo-1", "Este dato es obligatorio")));

        ResultActions result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisá los datos marcados"))
                .andExpect(jsonPath("$.errors.campo-1").value("Este dato es obligatorio"));
    }

    @Test
    @DisplayName("POST responde 400 cuando falta el objeto de valores")
    void crearShouldReturn400WhenValoresAreMissing() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/bloques/{id}/registros", BLOQUE_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content("{}"));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.valores").value("Los valores son obligatorios"));
        verifyNoInteractions(crearRegistroUseCase);
    }

    @Test
    @DisplayName("POST responde 400 cuando el id del bloque no es un UUID")
    void crearShouldReturn400WhenBloqueIdIsNotUuid() throws Exception {
        ResultActions result = mockMvc.perform(post("/api/bloques/no-es-uuid/registros")
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        result.andExpect(status().isBadRequest());
        verifyNoInteractions(crearRegistroUseCase);
    }

    @Test
    @DisplayName("GET lista la página con las claves que espera el frontend")
    void listarShouldReturnPageWithFrontendKeys() throws Exception {
        when(listarRegistrosUseCase.execute(any(ListarRegistrosRequest.class)))
                .thenReturn(new ListarRegistrosResponse(List.of(ITEM), 0, 20, 1, 1,
                        new TotalesItem(1, Map.of(CAMPO_ID, new BigDecimal("1500.5")))));

        ResultActions result = mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion));

        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(REGISTRO_ID.toString()))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.totales.cantidad").value(1))
                .andExpect(jsonPath("$.totales.sumas['" + CAMPO_ID + "']").value(1500.5));
    }

    @Test
    @DisplayName("GET pasa la búsqueda, el orden y la página al caso de uso")
    void listarShouldPassQueryParametersToUseCase() throws Exception {
        when(listarRegistrosUseCase.execute(any(ListarRegistrosRequest.class)))
                .thenReturn(new ListarRegistrosResponse(List.of(), 1, 5, 0, 1, new TotalesItem(0, Map.of())));

        mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion)
                .param("q", "harina").param("orden", "creadoEn").param("direccion", "asc")
                .param("pagina", "1").param("tamanio", "5"));

        ArgumentCaptor<ListarRegistrosRequest> request = ArgumentCaptor.forClass(ListarRegistrosRequest.class);
        verify(listarRegistrosUseCase).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().bloqueId()).isEqualTo(BLOQUE_ID);
        assertThat(request.getValue().texto()).isEqualTo("harina");
        assertThat(request.getValue().orden()).isEqualTo("creadoEn");
        assertThat(request.getValue().direccion()).isEqualTo("asc");
        assertThat(request.getValue().pagina()).isEqualTo(1);
        assertThat(request.getValue().tamanio()).isEqualTo(5);
    }

    @Test
    @DisplayName("GET responde 404 cuando el bloque no es del negocio")
    void listarShouldReturn404WhenBloqueIsNotFound() throws Exception {
        when(listarRegistrosUseCase.execute(any(ListarRegistrosRequest.class)))
                .thenThrow(new ResourceNotFoundException("El bloque no existe"));

        ResultActions result = mockMvc.perform(get("/api/bloques/{id}/registros", BLOQUE_ID).principal(sesion));

        result.andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("El bloque no existe"));
    }

    @Test
    @DisplayName("GET devuelve el registro")
    void obtenerShouldReturnRegistro() throws Exception {
        when(obtenerRegistroUseCase.execute(any(ObtenerRegistroRequest.class)))
                .thenReturn(new ObtenerRegistroResponse(ITEM));

        ResultActions result = mockMvc.perform(get("/api/registros/{id}", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(REGISTRO_ID.toString()));
        ArgumentCaptor<ObtenerRegistroRequest> request = ArgumentCaptor.forClass(ObtenerRegistroRequest.class);
        verify(obtenerRegistroUseCase).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("GET responde 404 cuando el registro no es del negocio")
    void obtenerShouldReturn404WhenRegistroIsNotFound() throws Exception {
        when(obtenerRegistroUseCase.execute(any(ObtenerRegistroRequest.class)))
                .thenThrow(new ResourceNotFoundException("El registro no existe"));

        ResultActions result = mockMvc.perform(get("/api/registros/{id}", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH edita el registro con el usuario de la sesión")
    void editarShouldReturnEditedRegistro() throws Exception {
        when(editarRegistroUseCase.execute(any(EditarRegistroRequest.class)))
                .thenReturn(new EditarRegistroResponse(ITEM));

        ResultActions result = mockMvc.perform(patch("/api/registros/{id}", REGISTRO_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        result.andExpect(status().isOk()).andExpect(jsonPath("$.valores.campo").value("dato"));
        ArgumentCaptor<EditarRegistroRequest> request = ArgumentCaptor.forClass(EditarRegistroRequest.class);
        verify(editarRegistroUseCase).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
        assertThat(request.getValue().valores()).containsEntry("campo", "dato");
    }

    @Test
    @DisplayName("PATCH responde 400 con el error por campo cuando los datos no son válidos")
    void editarShouldReturn400WithErrorsPerField() throws Exception {
        when(editarRegistroUseCase.execute(any(EditarRegistroRequest.class)))
                .thenThrow(new RegistroInvalidoException(Map.of("campo-1", "Tiene que ser un número")));

        ResultActions result = mockMvc.perform(patch("/api/registros/{id}", REGISTRO_ID)
                .principal(sesion).contentType(MediaType.APPLICATION_JSON).content(CUERPO));

        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.campo-1").value("Tiene que ser un número"));
    }

    @Test
    @DisplayName("DELETE elimina el registro y responde 204 sin cuerpo")
    void eliminarShouldReturn204() throws Exception {
        ResultActions result = mockMvc.perform(delete("/api/registros/{id}", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isNoContent());
        ArgumentCaptor<EliminarRegistroRequest> request = ArgumentCaptor.forClass(EliminarRegistroRequest.class);
        verify(eliminarRegistroUseCase).execute(request.capture());
        assertThat(request.getValue().usuarioId()).isEqualTo(USUARIO_ID);
        assertThat(request.getValue().registroId()).isEqualTo(REGISTRO_ID);
    }

    @Test
    @DisplayName("DELETE responde 404 cuando el registro no es del negocio")
    void eliminarShouldReturn404WhenRegistroIsNotFound() throws Exception {
        doThrow(new ResourceNotFoundException("El registro no existe"))
                .when(eliminarRegistroUseCase).execute(any(EliminarRegistroRequest.class));

        ResultActions result = mockMvc.perform(delete("/api/registros/{id}", REGISTRO_ID).principal(sesion));

        result.andExpect(status().isNotFound());
    }
}
