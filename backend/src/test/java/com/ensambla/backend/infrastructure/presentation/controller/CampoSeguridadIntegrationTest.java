package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CampoSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

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
    private UUID negocioId;
    private SesionHttpDePrueba sesion;
    private UUID bloqueId;
    private UUID otroBloqueId;
    private UUID campoId;
    private UUID negocioAjenoId;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = crearUsuario();
        negocioId = crearNegocio(usuarioId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Crear agrega el campo normalizado al final del bloque propio y lo deja guardado")
    void crearAgregaElCampoNormalizadoAlFinalDelBloquePropioYLoDejaGuardado() throws Exception {
        dadoUnBloquePropioConElCampo("Producto", 0);

        cuandoSeCreaUnCampoEnElBloque(bloqueId, """
                {"nombre":" Talle ","tipo":"OPCION","valorInicial":"Chico","opciones":[" Chico ","Grande"]}
                """);

        entoncesSeCreaElCampoNormalizadoAlFinal();
        entoncesElBloqueGuardaLosCamposEnOrden("Producto", "Talle");
    }

    @Test
    @DisplayName("Crear un campo de conexión con el propio bloque lo guarda con la tabla conectada")
    void crearUnCampoDeConexionConElPropioBloqueLoGuardaConLaTablaConectada() throws Exception {
        dadoUnBloquePropio();

        cuandoSeCreaUnCampoEnElBloque(bloqueId, """
                {"nombre":"Recomendado por","tipo":"RELACION","bloqueRelacionadoId":"%s"}
                """.formatted(bloqueId));

        entoncesSeGuardaLaConexionConElPropioBloque();
    }

    @Test
    @DisplayName("Crear un campo de conexión con una tabla de otro sistema devuelve 400 en bloqueRelacionadoId")
    void crearUnCampoDeConexionConUnaTablaDeOtroSistemaDevuelve400EnBloqueRelacionadoId() throws Exception {
        dadoUnBloquePropio();
        dadoOtroBloquePropioDeOtroSistema();

        cuandoSeCreaUnCampoEnElBloque(bloqueId, """
                {"nombre":"Cliente","tipo":"RELACION","bloqueRelacionadoId":"%s"}
                """.formatted(otroBloqueId));

        entoncesSeRechazaLaConexionConOtroSistema();
        entoncesElBloquePropioNoTieneCampos();
    }

    @Test
    @DisplayName("Listar devuelve los campos del bloque propio ordenados por posición")
    void listarDevuelveLosCamposDelBloquePropioOrdenadosPorPosicion() throws Exception {
        dadoUnBloquePropio();
        dadoUnCampoPropio("Segundo", 1);
        dadoUnCampoPropio("Primero", 0);

        cuandoSeListanLosCamposDelBloque(bloqueId);

        entoncesSeListanLosCamposEnOrden();
    }

    @Test
    @DisplayName("Un tipo fuera de la lista se rechaza con 400 y no guarda nada")
    void unTipoFueraDeLaListaSeRechazaCon400YNoGuardaNada() throws Exception {
        dadoUnBloquePropio();

        cuandoSeCreaUnCampoEnElBloque(bloqueId, "{\"nombre\":\"Adjunto\",\"tipo\":\"ARCHIVO\"}");

        entoncesSeRechazaElTipo();
        entoncesElBloquePropioNoTieneCampos();
    }

    @Test
    @DisplayName("Una opción sin lista de opciones se rechaza con 400")
    void unaOpcionSinListaDeOpcionesSeRechazaCon400() throws Exception {
        dadoUnBloquePropio();

        cuandoSeCreaUnCampoEnElBloque(bloqueId, "{\"nombre\":\"Talle\",\"tipo\":\"OPCION\"}");

        entoncesSeRechazanLasOpciones();
    }

    @Test
    @DisplayName("Crear con un nombre que ya existe en el bloque devuelve 409")
    void crearConUnNombreQueYaExisteEnElBloqueDevuelve409() throws Exception {
        dadoUnBloquePropioConElCampo("Producto", 0);

        cuandoSeCreaUnCampoEnElBloque(bloqueId, "{\"nombre\":\" PRODUCTO \",\"tipo\":\"TEXTO\"}");

        entoncesRespondeConflicto();
    }

    @Test
    @DisplayName("Editar reemplaza la configuración del campo propio")
    void editarReemplazaLaConfiguracionDelCampoPropio() throws Exception {
        dadoUnBloquePropioConElCampo("Precio", 0);

        cuandoSeEditaElCampo("""
                {"nombre":"Precio","tipo":"NUMERO","obligatorio":true,"valorInicial":"0"}
                """);

        entoncesElCampoPropioQuedaNumerico();
    }

    @Test
    @DisplayName("Crear en un bloque ajeno devuelve 404 y no guarda nada")
    void crearEnUnBloqueAjenoDevuelve404YNoGuardaNada() throws Exception {
        dadoUnBloqueAjeno();

        cuandoSeCreaUnCampoEnElBloque(bloqueId, "{\"nombre\":\"Intruso\",\"tipo\":\"TEXTO\"}");

        entoncesRespondeNoEncontrado();
        entoncesElBloqueAjenoNoTieneCampos();
    }

    @Test
    @DisplayName("Listar los campos de un bloque ajeno devuelve 404")
    void listarLosCamposDeUnBloqueAjenoDevuelve404() throws Exception {
        dadoUnBloqueAjenoConElCampo("Secreto");

        cuandoSeListanLosCamposDelBloque(bloqueId);

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un PATCH a un campo ajeno responde 404 y conserva su configuración")
    void unPatchAUnCampoAjenoResponde404YConservaSuConfiguracion() throws Exception {
        dadoUnBloqueAjenoConElCampo("Ajeno");

        cuandoSeEditaElCampo("{\"nombre\":\"Cambio\",\"tipo\":\"TEXTO\"}");

        entoncesRespondeNoEncontrado();
        entoncesElCampoAjenoConservaElNombre("Ajeno");
    }

    @Test
    @DisplayName("Un DELETE a un campo ajeno responde 404 y conserva el campo")
    void unDeleteAUnCampoAjenoResponde404YConservaElCampo() throws Exception {
        dadoUnBloqueAjenoConElCampo("Ajeno");

        cuandoSeEliminaElCampo();

        entoncesRespondeNoEncontrado();
        entoncesElCampoAjenoSigueExistiendo();
    }

    @Test
    @DisplayName("Un DELETE a un campo propio lo borra y responde 204")
    void unDeleteAUnCampoPropioLoBorraYResponde204() throws Exception {
        dadoUnBloquePropioConElCampo("Precio", 0);

        cuandoSeEliminaElCampo();

        entoncesSeEliminaElCampoPropio();
    }

    @Test
    @DisplayName("Crear un campo sin CSRF devuelve 403")
    void crearUnCampoSinCsrfDevuelve403() throws Exception {
        dadoUnBloquePropio();

        cuandoSeCreaUnCampoSinCsrf();

        entoncesSeProhibeSinGuardarCampos();
    }

    @Test
    @DisplayName("Listar campos exige una sesión válida")
    void listarCamposExigeUnaSesionValida() throws Exception {
        cuandoSeListanLosCamposSinSesion();

        entoncesNoAutoriza();
    }

    // --- dado ---

    private void dadoUnBloquePropio() {
        bloqueId = crearBloque(negocioId);
    }

    private void dadoOtroBloquePropioDeOtroSistema() {
        otroBloqueId = crearBloque(negocioId);
    }

    private void dadoUnBloquePropioConElCampo(String nombre, int posicion) {
        dadoUnBloquePropio();
        dadoUnCampoPropio(nombre, posicion);
    }

    private void dadoUnCampoPropio(String nombre, int posicion) {
        campoId = crearCampo(negocioId, bloqueId, nombre, posicion);
    }

    private void dadoUnBloqueAjeno() {
        negocioAjenoId = crearNegocio(crearUsuario());
        bloqueId = crearBloque(negocioAjenoId);
    }

    private void dadoUnBloqueAjenoConElCampo(String nombre) {
        dadoUnBloqueAjeno();
        campoId = crearCampo(negocioAjenoId, bloqueId, nombre, 0);
    }

    // --- cuando ---

    private void cuandoSeCreaUnCampoEnElBloque(UUID bloque, String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post("/api/bloques/{id}/campos", bloque).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeCreaUnCampoSinCsrf() throws Exception {
        respuesta = mockMvc.perform(post("/api/bloques/{id}/campos", bloqueId).session(sesion.sesion())
                .contentType("application/json").content("{\"nombre\":\"Producto\",\"tipo\":\"TEXTO\"}"));
    }

    private void cuandoSeListanLosCamposDelBloque(UUID bloque) throws Exception {
        respuesta = mockMvc.perform(get("/api/bloques/{id}/campos", bloque).session(sesion.sesion()));
    }

    private void cuandoSeListanLosCamposSinSesion() throws Exception {
        SesionHttpDePrueba sinUsuario = SesionHttpDePrueba.anonima(mockMvc);
        respuesta = mockMvc.perform(get("/api/bloques/{id}/campos", UUID.randomUUID()).session(sinUsuario.sesion()));
    }

    private void cuandoSeEditaElCampo(String cuerpo) throws Exception {
        respuesta = mockMvc.perform(patch("/api/campos/{id}", campoId).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeEliminaElCampo() throws Exception {
        respuesta = mockMvc.perform(delete("/api/campos/{id}", campoId).session(sesion.sesion())
                .header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    // --- entonces ---

    private void entoncesSeCreaElCampoNormalizadoAlFinal() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.bloqueId").value(bloqueId.toString()))
                .andExpect(jsonPath("$.nombre").value("Talle"))
                .andExpect(jsonPath("$.tipo").value("OPCION"))
                .andExpect(jsonPath("$.obligatorio").value(false))
                .andExpect(jsonPath("$.valorInicial").value("Chico"))
                .andExpect(jsonPath("$.opciones[0]").value("Chico"))
                .andExpect(jsonPath("$.posicion").value(1));
    }

    private void entoncesElBloqueGuardaLosCamposEnOrden(String primero, String segundo) {
        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId))
                .extracting(CampoEntity::getNombre)
                .containsExactly(primero, segundo);
    }

    private void entoncesSeGuardaLaConexionConElPropioBloque() throws Exception {
        respuesta.andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RELACION"))
                .andExpect(jsonPath("$.bloqueRelacionadoId").value(bloqueId.toString()));
        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId))
                .extracting(CampoEntity::getBloqueRelacionadoId)
                .containsExactly(bloqueId);
    }

    private void entoncesSeRechazaLaConexionConOtroSistema() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.bloqueRelacionadoId").value("Elegí una tabla de este sistema"));
    }

    private void entoncesElBloquePropioNoTieneCampos() {
        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId)).isEmpty();
    }

    private void entoncesSeListanLosCamposEnOrden() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Primero"))
                .andExpect(jsonPath("$[1].nombre").value("Segundo"))
                .andExpect(jsonPath("$[1].opciones").isArray());
    }

    private void entoncesSeRechazaElTipo() throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.tipo").exists());
    }

    private void entoncesSeRechazanLasOpciones() throws Exception {
        respuesta.andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.opciones").exists());
    }

    private void entoncesRespondeConflicto() throws Exception {
        respuesta.andExpect(status().isConflict());
    }

    private void entoncesElCampoPropioQuedaNumerico() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("NUMERO"))
                .andExpect(jsonPath("$.obligatorio").value(true))
                .andExpect(jsonPath("$.valorInicial").value("0"));
        assertThat(campos.findByIdAndNegocioId(campoId, negocioId).orElseThrow().getTipo())
                .isEqualTo(TipoCampo.NUMERO);
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesElBloqueAjenoNoTieneCampos() {
        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioAjenoId)).isEmpty();
    }

    private void entoncesElCampoAjenoConservaElNombre(String nombre) {
        assertThat(campos.findByIdAndNegocioId(campoId, negocioAjenoId).orElseThrow().getNombre())
                .isEqualTo(nombre);
    }

    private void entoncesElCampoAjenoSigueExistiendo() {
        assertThat(campos.findByIdAndNegocioId(campoId, negocioAjenoId)).isPresent();
    }

    private void entoncesSeEliminaElCampoPropio() throws Exception {
        respuesta.andExpect(status().isNoContent());
        assertThat(campos.findByIdAndNegocioId(campoId, negocioId)).isEmpty();
    }

    private void entoncesSeProhibeSinGuardarCampos() throws Exception {
        respuesta.andExpect(status().isForbidden());
        assertThat(campos.findByBloqueIdAndNegocioIdOrderByPosicionAscIdAsc(bloqueId, negocioId)).isEmpty();
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

    private UUID crearNegocio(UUID usuarioId) {
        return negocios.saveAndFlush(NegocioEntity.builder()
                .nombre("Negocio")
                .duenioId(usuarioId)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearBloque(UUID negocio) {
        UUID proyectoId = proyectos.saveAndFlush(ProyectoEntity.builder()
                .negocioId(negocio)
                .nombre("Inventario")
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
        return bloques.saveAndFlush(BloqueEntity.builder()
                .negocioId(negocio)
                .proyectoId(proyectoId)
                .nombre("Productos")
                .posicion((short) 0)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }

    private UUID crearCampo(UUID negocio, UUID bloque, String nombre, int posicion) {
        return campos.saveAndFlush(CampoEntity.builder()
                .negocioId(negocio)
                .bloqueId(bloque)
                .nombre(nombre)
                .tipo(TipoCampo.TEXTO)
                .obligatorio(false)
                .opciones(List.of())
                .posicion((short) posicion)
                .creadoEn(AHORA)
                .actualizadoEn(AHORA)
                .build()).getId();
    }
}
