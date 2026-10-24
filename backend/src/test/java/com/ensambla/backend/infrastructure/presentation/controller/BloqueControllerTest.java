package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloque;
import com.ensambla.backend.domain.useCase.bloque.crearBloque.CrearBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloque;
import com.ensambla.backend.domain.useCase.bloque.eliminarBloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloques;
import com.ensambla.backend.domain.useCase.bloque.listarBloques.ListarBloquesRequest;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloque;
import com.ensambla.backend.domain.useCase.bloque.renombrarBloque.RenombrarBloqueRequest;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.bloque.BloquePresentationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

class BloqueControllerTest {

    private static final UUID USUARIO_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID PROYECTO_ID = UUID.fromString("c0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final ListarBloques listarBloques = mock(ListarBloques.class);
    private final CrearBloque crearBloque = mock(CrearBloque.class);
    private final RenombrarBloque renombrarBloque = mock(RenombrarBloque.class);
    private final EliminarBloque eliminarBloque = mock(EliminarBloque.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new BloqueController(
                listarBloques,
                crearBloque,
                renombrarBloque,
                eliminarBloque,
                new BloquePresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los bloques del proyecto con todos sus datos")
    void listarShouldReturnMappedBloques() throws Exception {
        when(listarBloques.execute(any())).thenReturn(List.of(givenItem("Productos")));

        mockMvc.perform(get("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$[0].proyectoId").value(PROYECTO_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Productos"))
                .andExpect(jsonPath("$[0].posicion").value(0))
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists());
        verify(listarBloques).execute(new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando el proyecto no tiene bloques")
    void listarShouldReturnEmptyList() throws Exception {
        when(listarBloques.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET de un proyecto ajeno devuelve 404 estándar")
    void listarShouldReturn404WhenProyectoIsForeign() throws Exception {
        when(listarBloques.execute(any())).thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        mockMvc.perform(get("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Un id de proyecto que no es UUID no llega al caso de uso")
    void listarShouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/proyectos/invalido/bloques").principal(principal))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(listarBloques);
    }

    @Test
    @DisplayName("POST crea el bloque y devuelve 201 con su ubicación")
    void crearShouldReturnCreatedWithLocation() throws Exception {
        when(crearBloque.execute(any())).thenReturn(givenItem("Clientes"));

        mockMvc.perform(post("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"Clientes\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/bloques/" + BLOQUE_ID))
                .andExpect(jsonPath("$.nombre").value("Clientes"));
        verify(crearBloque).execute(new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Clientes"));
    }

    @Test
    @DisplayName("POST rechaza un nombre vacío antes de invocar el caso de uso")
    void crearShouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(crearBloque);
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 120 caracteres")
    void crearShouldRejectTooLongName() throws Exception {
        mockMvc.perform(post("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"" + "a".repeat(121) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(crearBloque);
    }

    @Test
    @DisplayName("POST con un nombre repetido en el proyecto devuelve 409")
    void crearShouldReturn409WhenNameIsTaken() throws Exception {
        when(crearBloque.execute(any()))
                .thenThrow(new DuplicateResourceException("Ya hay un bloque llamado \"Clientes\" en este proyecto"));

        mockMvc.perform(post("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"Clientes\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay un bloque llamado \"Clientes\" en este proyecto"));
    }

    @Test
    @DisplayName("PATCH delega el nuevo nombre con el bloque y el usuario de la sesión")
    void renombrarShouldPassRequest() throws Exception {
        when(renombrarBloque.execute(any())).thenReturn(givenItem("Compradores"));

        mockMvc.perform(patch("/api/bloques/{id}", BLOQUE_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"Compradores\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Compradores"));
        verify(renombrarBloque).execute(new RenombrarBloqueRequest(USUARIO_ID, BLOQUE_ID, "Compradores"));
    }

    @Test
    @DisplayName("PATCH rechaza un nombre vacío antes de invocar el caso de uso")
    void renombrarShouldRejectBlankName() throws Exception {
        mockMvc.perform(patch("/api/bloques/{id}", BLOQUE_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(renombrarBloque);
    }

    @Test
    @DisplayName("DELETE devuelve 204 después de delegar el borrado")
    void eliminarShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/bloques/{id}", BLOQUE_ID).principal(principal))
                .andExpect(status().isNoContent());

        verify(eliminarBloque).execute(new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID));
    }

    // --- arrange ---
    private BloqueItem givenItem(String nombre) {
        return new BloqueItem(BLOQUE_ID, PROYECTO_ID, nombre, (short) 0, AHORA, AHORA);
    }
}
