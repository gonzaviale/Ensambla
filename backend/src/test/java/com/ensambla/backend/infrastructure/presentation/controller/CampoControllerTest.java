package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.dto.campo.CrearCampoRequest;
import com.ensambla.backend.domain.dto.campo.EditarCampoRequest;
import com.ensambla.backend.domain.dto.campo.EliminarCampoRequest;
import com.ensambla.backend.domain.dto.campo.ListarCamposRequest;
import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.campo.CrearCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.EditarCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.EliminarCampoUseCase;
import com.ensambla.backend.domain.useCase.campo.ListarCamposUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.campo.CampoPresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CampoControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");
    private static final List<String> TALLES = List.of("Chico", "Grande");
    private static final String MENSAJE_TIPO = "Elegí un tipo válido: texto, número, fecha, opción o sí/no";
    private static final String BODY_TALLE = """
            {"nombre":"Talle","tipo":"OPCION","obligatorio":true,"valorInicial":"Chico","opciones":["Chico","Grande"]}
            """;

    private final ListarCamposUseCase listarCamposUseCase = mock(ListarCamposUseCase.class);
    private final CrearCampoUseCase crearCampoUseCase = mock(CrearCampoUseCase.class);
    private final EditarCampoUseCase editarCampoUseCase = mock(EditarCampoUseCase.class);
    private final EliminarCampoUseCase eliminarCampoUseCase = mock(EliminarCampoUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new CampoController(
                listarCamposUseCase,
                crearCampoUseCase,
                editarCampoUseCase,
                eliminarCampoUseCase,
                new CampoPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los campos del bloque con todos sus datos")
    void listarShouldReturnMappedCampos() throws Exception {
        when(listarCamposUseCase.execute(any())).thenReturn(List.of(givenItem("Talle")));

        mockMvc.perform(get("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CAMPO_ID.toString()))
                .andExpect(jsonPath("$[0].bloqueId").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Talle"))
                .andExpect(jsonPath("$[0].tipo").value("OPCION"))
                .andExpect(jsonPath("$[0].obligatorio").value(true))
                .andExpect(jsonPath("$[0].valorInicial").value("Chico"))
                .andExpect(jsonPath("$[0].opciones[1]").value("Grande"))
                .andExpect(jsonPath("$[0].posicion").value(0))
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists());
        verify(listarCamposUseCase).execute(new ListarCamposRequest(USUARIO_ID, BLOQUE_ID));
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando el bloque no tiene campos")
    void listarShouldReturnEmptyList() throws Exception {
        when(listarCamposUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET de un bloque ajeno devuelve 404 estándar")
    void listarShouldReturn404WhenBloqueIsForeign() throws Exception {
        when(listarCamposUseCase.execute(any())).thenThrow(new ResourceNotFoundException("El bloque no existe"));

        mockMvc.perform(get("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Un id de bloque que no es UUID no llega al caso de uso")
    void listarShouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/bloques/invalido/campos").principal(principal))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listarCamposUseCase);
    }

    @Test
    @DisplayName("POST crea el campo y devuelve 201 con su ubicación")
    void crearShouldReturnCreatedWithLocation() throws Exception {
        when(crearCampoUseCase.execute(any())).thenReturn(givenItem("Talle"));

        crear(BODY_TALLE)
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/campos/" + CAMPO_ID))
                .andExpect(jsonPath("$.nombre").value("Talle"));
        verify(crearCampoUseCase).execute(
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Talle", "OPCION", true, "Chico", TALLES));
    }

    @Test
    @DisplayName("POST deja en null lo que el body no manda para que el dominio aplique los valores por defecto")
    void crearShouldPassMissingOptionalsAsNull() throws Exception {
        when(crearCampoUseCase.execute(any())).thenReturn(givenItem("Producto"));

        crear("{\"nombre\":\"Producto\",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isCreated());
        verify(crearCampoUseCase).execute(
                new CrearCampoRequest(USUARIO_ID, BLOQUE_ID, "Producto", "TEXTO", null, null, null));
    }

    @Test
    @DisplayName("POST rechaza un nombre vacío antes de invocar el caso de uso")
    void crearShouldRejectBlankName() throws Exception {
        crear("{\"nombre\":\"  \",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(crearCampoUseCase);
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 120 caracteres")
    void crearShouldRejectTooLongName() throws Exception {
        crear("{\"nombre\":\"" + "a".repeat(121) + "\",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(crearCampoUseCase);
    }

    @Test
    @DisplayName("POST con un tipo fuera de la lista devuelve 400 con el error en tipo")
    void crearShouldReturn400WhenTipoIsUnknown() throws Exception {
        givenCrearRechaza("tipo", MENSAJE_TIPO);

        crear("{\"nombre\":\"Dato\",\"tipo\":\"MONEDA\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Revisá la configuración del campo"))
                .andExpect(jsonPath("$.errors.tipo").value(MENSAJE_TIPO));
    }

    @Test
    @DisplayName("POST de una opción sin opciones devuelve 400 con el error en opciones")
    void crearShouldReturn400WhenOpcionesAreMissing() throws Exception {
        givenCrearRechaza("opciones", "Agregá al menos una opción");

        crear("{\"nombre\":\"Talle\",\"tipo\":\"OPCION\",\"opciones\":[]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.opciones").value("Agregá al menos una opción"));
    }

    @Test
    @DisplayName("POST con un valor inicial que no corresponde al tipo devuelve 400 con el error en valorInicial")
    void crearShouldReturn400WhenValorInicialIsInvalid() throws Exception {
        givenCrearRechaza("valorInicial", "Tiene que ser un número, por ejemplo 12.5");

        crear("{\"nombre\":\"Precio\",\"tipo\":\"NUMERO\",\"valorInicial\":\"doce\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.valorInicial").value("Tiene que ser un número, por ejemplo 12.5"));
    }

    @Test
    @DisplayName("POST con un nombre repetido en el bloque devuelve 409")
    void crearShouldReturn409WhenNameIsTaken() throws Exception {
        when(crearCampoUseCase.execute(any()))
                .thenThrow(new DuplicateResourceException("Ya hay un campo llamado \"Talle\" en este bloque"));

        crear(BODY_TALLE)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay un campo llamado \"Talle\" en este bloque"));
    }

    @Test
    @DisplayName("POST en un bloque ajeno devuelve 404")
    void crearShouldReturn404WhenBloqueIsForeign() throws Exception {
        when(crearCampoUseCase.execute(any())).thenThrow(new ResourceNotFoundException("El bloque no existe"));

        crear(BODY_TALLE)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH delega la configuración completa con el campo y el usuario de la sesión")
    void editarShouldPassRequest() throws Exception {
        when(editarCampoUseCase.execute(any())).thenReturn(givenItem("Talle"));

        editar(BODY_TALLE)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Talle"));
        verify(editarCampoUseCase).execute(
                new EditarCampoRequest(USUARIO_ID, CAMPO_ID, "Talle", "OPCION", true, "Chico", TALLES));
    }

    @Test
    @DisplayName("PATCH rechaza un nombre vacío antes de invocar el caso de uso")
    void editarShouldRejectBlankName() throws Exception {
        editar("{\"nombre\":\"\",\"tipo\":\"TEXTO\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(editarCampoUseCase);
    }

    @Test
    @DisplayName("PATCH con una configuración inválida devuelve 400 con el error por dato")
    void editarShouldReturn400WhenConfiguracionIsInvalid() throws Exception {
        when(editarCampoUseCase.execute(any()))
                .thenThrow(new CampoInvalidoException(Map.of("opciones", "Hay opciones repetidas")));

        editar("{\"nombre\":\"Talle\",\"tipo\":\"OPCION\",\"opciones\":[\"A\",\"a\"]}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.opciones").value("Hay opciones repetidas"));
    }

    @Test
    @DisplayName("PATCH con el nombre de otro campo devuelve 409")
    void editarShouldReturn409WhenNameIsTaken() throws Exception {
        when(editarCampoUseCase.execute(any()))
                .thenThrow(new DuplicateResourceException("Ya hay un campo llamado \"Talle\" en este bloque"));

        editar(BODY_TALLE)
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("PATCH de un campo ajeno devuelve 404")
    void editarShouldReturn404WhenCampoIsForeign() throws Exception {
        when(editarCampoUseCase.execute(any())).thenThrow(new ResourceNotFoundException("El campo no existe"));

        editar(BODY_TALLE)
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un id de campo que no es UUID no llega al caso de uso de editar")
    void editarShouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(patch("/api/campos/invalido").principal(principal)
                        .contentType("application/json").content(BODY_TALLE))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(editarCampoUseCase);
    }

    @Test
    @DisplayName("DELETE devuelve 204 después de delegar el borrado")
    void eliminarShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/campos/{id}", CAMPO_ID).principal(principal))
                .andExpect(status().isNoContent());

        verify(eliminarCampoUseCase).execute(new EliminarCampoRequest(USUARIO_ID, CAMPO_ID));
    }

    @Test
    @DisplayName("DELETE de un campo ajeno devuelve 404")
    void eliminarShouldReturn404WhenCampoIsForeign() throws Exception {
        givenEliminarNoEncuentra();

        mockMvc.perform(delete("/api/campos/{id}", CAMPO_ID).principal(principal))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un id de campo que no es UUID no llega al caso de uso de eliminar")
    void eliminarShouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(delete("/api/campos/invalido").principal(principal))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eliminarCampoUseCase);
    }

    // --- arrange ---
    private void givenCrearRechaza(String dato, String mensaje) {
        when(crearCampoUseCase.execute(any())).thenThrow(new CampoInvalidoException(Map.of(dato, mensaje)));
    }

    private void givenEliminarNoEncuentra() {
        doThrow(new ResourceNotFoundException("El campo no existe"))
                .when(eliminarCampoUseCase).execute(any());
    }

    private CampoItem givenItem(String nombre) {
        return new CampoItem(CAMPO_ID, BLOQUE_ID, nombre, "OPCION", true, "Chico", TALLES, (short) 0, AHORA, AHORA);
    }

    // --- act ---
    private ResultActions crear(String body) throws Exception {
        return mockMvc.perform(post("/api/bloques/{id}/campos", BLOQUE_ID).principal(principal)
                .contentType("application/json").content(body));
    }

    private ResultActions editar(String body) throws Exception {
        return mockMvc.perform(patch("/api/campos/{id}", CAMPO_ID).principal(principal)
                .contentType("application/json").content(body));
    }
}
