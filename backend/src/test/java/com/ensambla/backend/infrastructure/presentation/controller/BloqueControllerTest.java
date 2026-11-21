package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
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
import com.ensambla.backend.support.MockMvcDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.OffsetDateTime;
import java.util.List;

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

    private static final UsuarioId USUARIO_ID = UsuarioId.de("00000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T12:00:00Z");

    private final ListarBloques listarBloques = mock(ListarBloques.class);
    private final CrearBloque crearBloque = mock(CrearBloque.class);
    private final RenombrarBloque renombrarBloque = mock(RenombrarBloque.class);
    private final EliminarBloque eliminarBloque = mock(EliminarBloque.class);
    private final UsernamePasswordAuthenticationToken principal =
            new UsernamePasswordAuthenticationToken(USUARIO_ID.toString(), null);

    private MockMvc mockMvc;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() {
        var controller = new BloqueController(
                listarBloques,
                crearBloque,
                renombrarBloque,
                eliminarBloque,
                new BloquePresentationMapper());
        mockMvc = MockMvcDePrueba.standalone(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET lista los bloques del proyecto con todos sus datos")
    void getListaLosBloquesDelProyectoConTodosSusDatos() throws Exception {
        dadoUnProyectoConElBloque("Productos");

        cuandoSeListanLosBloquesDelProyecto();

        entoncesSeListaElBloqueConTodosSusDatos();
        entoncesSeConsultaElProyectoDelUsuario();
    }

    @Test
    @DisplayName("GET devuelve una lista vacía cuando el proyecto no tiene bloques")
    void getDevuelveUnaListaVaciaCuandoElProyectoNoTieneBloques() throws Exception {
        dadoUnProyectoSinBloques();

        cuandoSeListanLosBloquesDelProyecto();

        entoncesSeDevuelveUnaListaVacia();
    }

    @Test
    @DisplayName("GET de un proyecto ajeno devuelve 404 estándar")
    void getDeUnProyectoAjenoDevuelve404Estandar() throws Exception {
        dadoUnProyectoAjeno();

        cuandoSeListanLosBloquesDelProyecto();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un id de proyecto que no es UUID no llega al caso de uso")
    void unIdDeProyectoQueNoEsUuidNoLlegaAlCasoDeUso() throws Exception {
        cuandoSeListanLosBloquesDelProyectoConIdInvalido();

        entoncesSeRechazaElListadoSinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("POST crea el bloque y devuelve 201 con su ubicación")
    void postCreaElBloqueYDevuelve201ConSuUbicacion() throws Exception {
        dadoUnBloqueCreable("Clientes");

        cuandoSeCreaElBloque("Clientes");

        entoncesSeCreaElBloqueConSuUbicacion();
        entoncesElCasoDeUsoRecibeElBloqueANombrar("Clientes");
    }

    @Test
    @DisplayName("POST rechaza un nombre vacío antes de invocar el caso de uso")
    void postRechazaUnNombreVacioAntesDeInvocarElCasoDeUso() throws Exception {
        cuandoSeCreaElBloque("  ");

        entoncesSeRechazaElNombreSinCrearElBloque();
    }

    @Test
    @DisplayName("POST rechaza un nombre de más de 120 caracteres")
    void postRechazaUnNombreDeMasDe120Caracteres() throws Exception {
        cuandoSeCreaElBloque("a".repeat(121));

        entoncesSeRechazaElNombreSinCrearElBloque();
    }

    @Test
    @DisplayName("POST con un nombre repetido en el proyecto devuelve 409")
    void postConUnNombreRepetidoEnElProyectoDevuelve409() throws Exception {
        dadoUnNombreRepetidoEnElProyecto();

        cuandoSeCreaElBloque("Clientes");

        entoncesRespondeConflictoPorElNombreRepetido();
    }

    @Test
    @DisplayName("PATCH delega el nuevo nombre con el bloque y el usuario de la sesión")
    void patchDelegaElNuevoNombreConElBloqueYElUsuarioDeLaSesion() throws Exception {
        dadoUnBloqueRenombrable("Compradores");

        cuandoSeRenombraElBloque("Compradores");

        entoncesSeDevuelveElBloqueRenombrado("Compradores");
        entoncesElCasoDeUsoRecibeElNuevoNombre("Compradores");
    }

    @Test
    @DisplayName("PATCH rechaza un nombre vacío antes de invocar el caso de uso")
    void patchRechazaUnNombreVacioAntesDeInvocarElCasoDeUso() throws Exception {
        cuandoSeRenombraElBloque("");

        entoncesSeRechazaElRenombreSinLlegarAlCasoDeUso();
    }

    @Test
    @DisplayName("DELETE devuelve 204 después de delegar el borrado")
    void deleteDevuelve204DespuesDeDelegarElBorrado() throws Exception {
        cuandoSeEliminaElBloque();

        entoncesSeEliminaElBloqueDelUsuario();
    }

    // --- dado ---

    private void dadoUnProyectoConElBloque(String nombre) {
        when(listarBloques.execute(any())).thenReturn(List.of(unBloque(nombre)));
    }

    private void dadoUnProyectoSinBloques() {
        when(listarBloques.execute(any())).thenReturn(List.of());
    }

    private void dadoUnProyectoAjeno() {
        when(listarBloques.execute(any())).thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void dadoUnBloqueCreable(String nombre) {
        when(crearBloque.execute(any())).thenReturn(unBloque(nombre));
    }

    private void dadoUnNombreRepetidoEnElProyecto() {
        when(crearBloque.execute(any()))
                .thenThrow(new DuplicateResourceException("Ya hay un bloque llamado \"Clientes\" en este proyecto"));
    }

    private void dadoUnBloqueRenombrable(String nombre) {
        when(renombrarBloque.execute(any())).thenReturn(unBloque(nombre));
    }

    // --- cuando ---

    private void cuandoSeListanLosBloquesDelProyecto() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal));
    }

    private void cuandoSeListanLosBloquesDelProyectoConIdInvalido() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/invalido/bloques").principal(principal));
    }

    private void cuandoSeCreaElBloque(String nombre) throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos/{id}/bloques", PROYECTO_ID).principal(principal)
                .contentType("application/json").content("{\"nombre\":\"" + nombre + "\"}"));
    }

    private void cuandoSeRenombraElBloque(String nombre) throws Exception {
        respuesta = mockMvc.perform(patch("/api/bloques/{id}", BLOQUE_ID).principal(principal)
                .contentType("application/json").content("{\"nombre\":\"" + nombre + "\"}"));
    }

    private void cuandoSeEliminaElBloque() throws Exception {
        respuesta = mockMvc.perform(delete("/api/bloques/{id}", BLOQUE_ID).principal(principal));
    }

    // --- entonces ---

    private void entoncesSeListaElBloqueConTodosSusDatos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(BLOQUE_ID.toString()))
                .andExpect(jsonPath("$[0].proyectoId").value(PROYECTO_ID.toString()))
                .andExpect(jsonPath("$[0].nombre").value("Productos"))
                .andExpect(jsonPath("$[0].posicion").value(0))
                .andExpect(jsonPath("$[0].creadoEn").exists())
                .andExpect(jsonPath("$[0].actualizadoEn").exists());
    }

    private void entoncesSeConsultaElProyectoDelUsuario() {
        verify(listarBloques).execute(new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesSeDevuelveUnaListaVacia() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    private void entoncesSeRechazaElListadoSinLlegarAlCasoDeUso() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(listarBloques);
    }

    private void entoncesSeCreaElBloqueConSuUbicacion() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/bloques/" + BLOQUE_ID))
                .andExpect(jsonPath("$.nombre").value("Clientes"));
    }

    private void entoncesElCasoDeUsoRecibeElBloqueANombrar(String nombre) {
        verify(crearBloque).execute(new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, nombre));
    }

    private void entoncesSeRechazaElNombreSinCrearElBloque() throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.nombre").exists());
        verifyNoInteractions(crearBloque);
    }

    private void entoncesRespondeConflictoPorElNombreRepetido() throws Exception {
        respuesta.andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya hay un bloque llamado \"Clientes\" en este proyecto"));
    }

    private void entoncesSeDevuelveElBloqueRenombrado(String nombre) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value(nombre));
    }

    private void entoncesElCasoDeUsoRecibeElNuevoNombre(String nombre) {
        verify(renombrarBloque).execute(new RenombrarBloqueRequest(USUARIO_ID, BLOQUE_ID, nombre));
    }

    private void entoncesSeRechazaElRenombreSinLlegarAlCasoDeUso() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        verifyNoInteractions(renombrarBloque);
    }

    private void entoncesSeEliminaElBloqueDelUsuario() throws Exception {
        respuesta.andExpect(status().isNoContent());
        verify(eliminarBloque).execute(new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID));
    }

    // --- datos ---

    private BloqueItem unBloque(String nombre) {
        return new BloqueItem(BLOQUE_ID, PROYECTO_ID, nombre, (short) 0, AHORA, AHORA);
    }
}
