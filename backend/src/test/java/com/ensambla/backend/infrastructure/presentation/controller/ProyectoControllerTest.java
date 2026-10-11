package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.proyecto.CrearProyectoDesdePlantillaRequest;
import com.ensambla.backend.domain.dto.proyecto.CrearProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.EliminarProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ListarProyectosRequest;
import com.ensambla.backend.domain.dto.proyecto.ObtenerProyectoRequest;
import com.ensambla.backend.domain.dto.proyecto.ProyectoItem;
import com.ensambla.backend.domain.dto.proyecto.RenombrarProyectoRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.mapper.proyecto.ProyectoItemMapper;
import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoDesdePlantillaUseCase;
import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.EliminarProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ListarProyectosUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ObtenerProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.RenombrarProyectoUseCase;
import com.ensambla.backend.infrastructure.presentation.exception.GlobalExceptionHandler;
import com.ensambla.backend.infrastructure.presentation.mapper.proyecto.ProyectoPresentationMapper;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
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

class ProyectoControllerTest {

    private final ListarProyectosUseCase listarProyectosUseCase = mock(ListarProyectosUseCase.class);
    private final ObtenerProyectoUseCase obtenerProyectoUseCase = mock(ObtenerProyectoUseCase.class);
    private final CrearProyectoUseCase crearProyectoUseCase = mock(CrearProyectoUseCase.class);
    private final RenombrarProyectoUseCase renombrarProyectoUseCase = mock(RenombrarProyectoUseCase.class);
    private final CrearProyectoDesdePlantillaUseCase crearProyectoDesdePlantillaUseCase = mock(CrearProyectoDesdePlantillaUseCase.class);
    private final EliminarProyectoUseCase eliminarProyectoUseCase = mock(EliminarProyectoUseCase.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        var controller = new ProyectoController(
                listarProyectosUseCase,
                obtenerProyectoUseCase,
                crearProyectoUseCase,
                renombrarProyectoUseCase,
                crearProyectoDesdePlantillaUseCase,
                eliminarProyectoUseCase,
                new ProyectoPresentationMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista proyectos y mapea todos los datos del árbol a JSON")
    void listarShouldReturnMappedTree() throws Exception {
        when(listarProyectosUseCase.execute(any())).thenReturn(List.of(givenResponse()));

        mockMvc.perform(get("/api/proyectos").principal(principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(PROYECTO_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Inventario"))
                .andExpect(jsonPath("$[0].descripcion").value("Mis productos"))
                .andExpect(jsonPath("$[0].plantillaId").value(PLANTILLA_ID.toString()))
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists())
                .andExpect(jsonPath("$[0].bloques[0].id").exists())
                .andExpect(jsonPath("$[0].bloques[0].nombre").value("Productos"))
                .andExpect(jsonPath("$[0].bloques[0].posicion").value(0))
                .andExpect(jsonPath("$[0].bloques[0].campos[0].id").exists())
                .andExpect(jsonPath("$[0].bloques[0].campos[0].nombre").value("Nombre"))
                .andExpect(jsonPath("$[0].bloques[0].campos[0].tipo").value("TEXTO"))
                .andExpect(jsonPath("$[0].bloques[0].campos[0].obligatorio").value(true))
                .andExpect(jsonPath("$[0].bloques[0].campos[0].opciones").isEmpty())
                .andExpect(jsonPath("$[0].bloques[0].campos[0].posicion").value(0));
        verify(listarProyectosUseCase).execute(new ListarProyectosRequest(USUARIO_ID));
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando no hay proyectos")
    void listarShouldReturnEmptyList() throws Exception {
        when(listarProyectosUseCase.execute(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/proyectos").principal(principal))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("GET por id delega usando el usuario de sesión")
    void obtenerShouldPassSessionUsuario() throws Exception {
        when(obtenerProyectoUseCase.execute(any())).thenReturn(givenResponse());

        mockMvc.perform(get("/api/proyectos/{id}", PROYECTO_ID).principal(principal))
                .andExpect(status().isOk());
        verify(obtenerProyectoUseCase).execute(new ObtenerProyectoRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("POST crea en blanco sin aceptar el negocio indicado por el navegador")
    void crearShouldIgnoreClientNegocio() throws Exception {
        when(crearProyectoUseCase.execute(any())).thenReturn(givenResponse());

        mockMvc.perform(post("/api/proyectos").principal(principal).contentType("application/json")
                        .content("{\"nombre\":\"Nuevo\",\"descripcion\":null,\"negocioId\":\"otro\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/proyectos/" + PROYECTO_ID));
        verify(crearProyectoUseCase).execute(new CrearProyectoRequest(USUARIO_ID, "Nuevo", null));
    }

    @Test
    @DisplayName("POST desde plantilla usa el id de plantilla y devuelve la ubicación creada")
    void desdePlantillaShouldCreateProyecto() throws Exception {
        when(crearProyectoDesdePlantillaUseCase.execute(any())).thenReturn(givenResponse());

        mockMvc.perform(post("/api/proyectos/desde-plantilla/{id}", PLANTILLA_ID).principal(principal))
                .andExpect(status().isCreated());
        verify(crearProyectoDesdePlantillaUseCase)
                .execute(new CrearProyectoDesdePlantillaRequest(USUARIO_ID, PLANTILLA_ID));
    }

    @Test
    @DisplayName("PATCH delega el nuevo nombre junto con el id y el usuario")
    void renombrarShouldPassRequest() throws Exception {
        when(renombrarProyectoUseCase.execute(any())).thenReturn(givenResponse());

        mockMvc.perform(patch("/api/proyectos/{id}", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"Otro\"}"))
                .andExpect(status().isOk());
        verify(renombrarProyectoUseCase).execute(new RenombrarProyectoRequest(USUARIO_ID, PROYECTO_ID, "Otro"));
    }

    @Test
    @DisplayName("DELETE devuelve 204 después de delegar el borrado")
    void eliminarShouldReturn204() throws Exception {
        mockMvc.perform(delete("/api/proyectos/{id}", PROYECTO_ID).principal(principal))
                .andExpect(status().isNoContent());

        verify(eliminarProyectoUseCase).execute(new EliminarProyectoRequest(USUARIO_ID, PROYECTO_ID));
    }

    @Test
    @DisplayName("Un UUID inválido no llega al caso de uso")
    void obtenerShouldRejectInvalidUuid() throws Exception {
        mockMvc.perform(get("/api/proyectos/invalido").principal(principal)).andExpect(status().isBadRequest());

        verifyNoInteractions(obtenerProyectoUseCase);
    }

    @Test
    @DisplayName("Crear rechaza un nombre vacío antes de invocar el caso de uso")
    void crearShouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/api/proyectos").principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"  \"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombre").exists());

        verifyNoInteractions(crearProyectoUseCase);
    }

    @Test
    @DisplayName("Renombrar rechaza un nombre vacío antes de invocar el caso de uso")
    void renombrarShouldRejectBlankName() throws Exception {
        mockMvc.perform(patch("/api/proyectos/{id}", PROYECTO_ID).principal(principal)
                        .contentType("application/json").content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(renombrarProyectoUseCase);
    }

    @Test
    @DisplayName("Un proyecto ajeno devuelve 404 estándar")
    void obtenerShouldReturn404() throws Exception {
        when(obtenerProyectoUseCase.execute(any())).thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        mockMvc.perform(get("/api/proyectos/{id}", PROYECTO_ID).principal(principal))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    // --- arrange ---
    private ProyectoItem givenResponse() {
        return new ProyectoItemMapper().toResponse(ProyectoTestData.proyecto());
    }
}

