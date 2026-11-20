package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import com.ensambla.backend.infrastructure.repository.entity.CampoEntity;
import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IBloqueJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.ICampoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.INegocioJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IProyectoJpaRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IUsuarioJpaRepository;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class BloqueSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private IUsuarioJpaRepository usuarios;

    @Autowired
    private INegocioJpaRepository negocios;

    @Autowired
    private IProyectoJpaRepository proyectos;

    @Autowired
    private IBloqueJpaRepository bloques;

    @Autowired
    private ICampoJpaRepository campos;

    private MockMvc mockMvc;
    private NegocioId negocioId;
    private SesionHttpDePrueba sesion;
    private ProyectoId proyectoId;
    private UUID bloqueId;
    private UUID otroBloqueId;
    private UUID negocioAjenoId;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UsuarioId usuarioId = UsuarioId.de(crearUsuario());
        negocioId = NegocioId.de(crearNegocio(usuarioId));
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Crear agrega el bloque al final del proyecto propio y lo deja guardado")
    void crearAgregaElBloqueAlFinalDelProyectoPropioYLoDejaGuardado() throws Exception {
        dadoUnProyectoPropioConElBloque("Productos");

        cuandoSeCreaUnBloque(proyectoId.valor(), " Clientes ");

        entoncesSeCreaElBloqueAlFinal();
        entoncesElProyectoGuardaLosBloquesEnOrden("Productos", "Clientes");
    }

    @Test
    @DisplayName("Crear con un nombre que ya existe en el proyecto devuelve 409")
    void crearConUnNombreQueYaExisteEnElProyectoDevuelve409() throws Exception {
        dadoUnProyectoPropioConElBloque("Productos");

        cuandoSeCreaUnBloque(proyectoId.valor(), "productos");

        entoncesRespondeConflicto();
    }

    @Test
    @DisplayName("Crear en un proyecto ajeno devuelve 404 y no guarda nada")
    void crearEnUnProyectoAjenoDevuelve404YNoGuardaNada() throws Exception {
        dadoUnProyectoAjeno();

        cuandoSeCreaUnBloque(proyectoId.valor(), "Intruso");

        entoncesRespondeNoEncontrado();
        entoncesElProyectoAjenoNoTieneBloques();
    }

    @Test
    @DisplayName("Listar los bloques de un proyecto ajeno devuelve 404")
    void listarLosBloquesDeUnProyectoAjenoDevuelve404() throws Exception {
        dadoUnProyectoAjenoConElBloque("Secreto");

        cuandoSeListanLosBloquesDelProyecto();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un PATCH a un bloque ajeno responde 404 y conserva el nombre")
    void unPatchAUnBloqueAjenoResponde404YConservaElNombre() throws Exception {
        dadoUnProyectoAjenoConElBloque("Ajeno");

        cuandoSeRenombraElBloque("Cambio");

        entoncesRespondeNoEncontrado();
        entoncesElBloqueAjenoConservaElNombre("Ajeno");
    }

    @Test
    @DisplayName("Un DELETE a un bloque ajeno responde 404 y conserva el bloque")
    void unDeleteAUnBloqueAjenoResponde404YConservaElBloque() throws Exception {
        dadoUnProyectoAjenoConElBloque("Ajeno");

        cuandoSeEliminaElBloque();

        entoncesRespondeNoEncontrado();
        entoncesElBloqueAjenoSigueExistiendo();
    }

    @Test
    @DisplayName("Un DELETE a una tabla que otra tabla usa en una conexión responde 400 y conserva la tabla")
    void unDeleteAUnaTablaQueOtraTablaUsaEnUnaConexionResponde400YConservaLaTabla() throws Exception {
        dadoUnaTablaDeClientesUsadaPorLaTablaDePedidos();

        cuandoSeEliminaElBloque();

        entoncesSeRechazaPorqueLaTablaSeUsaEnUnaConexion();
        entoncesLaTablaPropiaSigueExistiendo();
    }

    @Test
    @DisplayName("Un DELETE a una tabla que sólo se conecta consigo misma la borra")
    void unDeleteAUnaTablaQueSoloSeConectaConsigoMismaLaBorra() throws Exception {
        dadoUnaTablaConectadaConsigoMisma();

        cuandoSeEliminaElBloque();

        entoncesSeEliminaLaTablaPropia();
    }

    @Test
    @DisplayName("Crear un bloque sin CSRF devuelve 403")
    void crearUnBloqueSinCsrfDevuelve403() throws Exception {
        dadoUnProyectoPropio();

        cuandoSeCreaUnBloqueSinCsrf();

        entoncesSeProhibeSinGuardarBloques();
    }

    @Test
    @DisplayName("Listar bloques exige una sesión válida")
    void listarBloquesExigeUnaSesionValida() throws Exception {
        cuandoSeListanLosBloquesSinSesion();

        entoncesNoAutoriza();
    }

    // --- dado ---

    private void dadoUnProyectoPropio() {
        proyectoId = ProyectoId.de(crearProyecto(negocioId));
    }

    private void dadoUnProyectoPropioConElBloque(String nombre) {
        dadoUnProyectoPropio();
        bloqueId = crearBloque(negocioId, proyectoId, nombre, 0);
    }

    private void dadoUnProyectoAjeno() {
        negocioAjenoId = crearNegocio(UsuarioId.de(crearUsuario()));
        proyectoId = ProyectoId.de(crearProyecto(NegocioId.de(negocioAjenoId)));
    }

    private void dadoUnProyectoAjenoConElBloque(String nombre) {
        dadoUnProyectoAjeno();
        bloqueId = crearBloque(NegocioId.de(negocioAjenoId), proyectoId, nombre, 0);
    }

    private void dadoUnaTablaDeClientesUsadaPorLaTablaDePedidos() {
        dadoUnProyectoPropio();
        bloqueId = crearBloque(negocioId, proyectoId, "Clientes", 0);
        otroBloqueId = crearBloque(negocioId, proyectoId, "Pedidos", 1);
        crearConexion(BloqueId.de(otroBloqueId), "Cliente", bloqueId);
    }

    private void dadoUnaTablaConectadaConsigoMisma() {
        dadoUnProyectoPropio();
        bloqueId = crearBloque(negocioId, proyectoId, "Clientes", 0);
        crearConexion(BloqueId.de(bloqueId), "Recomendado por", bloqueId);
    }

    // --- cuando ---

    private void cuandoSeCreaUnBloque(UUID proyecto, String nombre) throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos/{id}/bloques", proyecto).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content("{\"nombre\":\"" + nombre + "\"}"));
    }

    private void cuandoSeCreaUnBloqueSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos/{id}/bloques", proyectoId).session(sesion.sesion())
                .contentType("application/json").content("{\"nombre\":\"Clientes\"}"));
    }

    private void cuandoSeListanLosBloquesDelProyecto() throws Exception {
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/bloques", proyectoId).session(sesion.sesion()));
    }

    private void cuandoSeListanLosBloquesSinSesion() throws Exception {
        SesionHttpDePrueba sinUsuario = SesionHttpDePrueba.anonima(mockMvc);
        respuesta = mockMvc.perform(get("/api/proyectos/{id}/bloques", UUID.randomUUID())
                .session(sinUsuario.sesion()));
    }

    private void cuandoSeRenombraElBloque(String nombre) throws Exception {
        respuesta = mockMvc.perform(patch("/api/bloques/{id}", bloqueId).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content("{\"nombre\":\"" + nombre + "\"}"));
    }

    private void cuandoSeEliminaElBloque() throws Exception {
        respuesta = mockMvc.perform(delete("/api/bloques/{id}", bloqueId).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    // --- entonces ---

    private void entoncesSeCreaElBloqueAlFinal() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Clientes"))
                .andExpect(jsonPath("$.posicion").value(1));
    }

    private void entoncesElProyectoGuardaLosBloquesEnOrden(String primero, String segundo) {
        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(),
                negocioId.valor()))
                .extracting(BloqueEntity::getNombre)
                .containsExactly(primero, segundo);
    }

    private void entoncesRespondeConflicto() throws Exception {
        respuesta.andExpect(status().isConflict());
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesElProyectoAjenoNoTieneBloques() {
        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(), negocioAjenoId))
                .isEmpty();
    }

    private void entoncesElBloqueAjenoConservaElNombre(String nombre) {
        assertThat(bloques.findByIdAndNegocioId(bloqueId, negocioAjenoId).orElseThrow().getNombre())
                .isEqualTo(nombre);
    }

    private void entoncesElBloqueAjenoSigueExistiendo() {
        assertThat(bloques.findByIdAndNegocioId(bloqueId, negocioAjenoId)).isPresent();
    }

    private void entoncesSeRechazaPorqueLaTablaSeUsaEnUnaConexion() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("La tabla «Clientes» se usa en la columna «Cliente» de «Pedidos»"));
    }

    private void entoncesLaTablaPropiaSigueExistiendo() {
        assertThat(bloques.findByIdAndNegocioId(bloqueId, negocioId.valor())).isPresent();
    }

    private void entoncesSeEliminaLaTablaPropia() throws Exception {
        respuesta.andExpect(status().isNoContent());
        assertThat(bloques.findByIdAndNegocioId(bloqueId, negocioId.valor())).isEmpty();
    }

    private void entoncesSeProhibeSinGuardarBloques() throws Exception {
        respuesta.andExpect(status().isForbidden());
        assertThat(bloques.findAllByProyectoIdAndNegocioIdOrderByPosicionAscIdAsc(proyectoId.valor(),
                negocioId.valor())).isEmpty();
    }

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    // --- datos ---

    private UUID crearUsuario() {
        return usuarios.saveAndFlush(UsuarioEntity.builder()
                .nombre("Persona")
                .email(UUID.randomUUID() + "@example.com")
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearNegocio(UsuarioId usuarioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId.valor())
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearProyecto(NegocioId negocio) {
        return proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocio.valor())
                .nombre("Inventario")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearBloque(NegocioId negocio, ProyectoId proyecto, String nombre, int posicion) {
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocio.valor())
                .proyectoId(proyecto.valor())
                .nombre(nombre)
                .posicion((short) posicion)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private void crearConexion(BloqueId bloque, String nombre, UUID relacionadoId) {
        campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocioId.valor())
                .bloqueId(bloque.valor())
                .nombre(nombre)
                .tipo(TipoCampo.RELACION)
                .obligatorio(false)
                .opciones(List.of())
                .bloqueRelacionadoId(relacionadoId)
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build());
    }
}
