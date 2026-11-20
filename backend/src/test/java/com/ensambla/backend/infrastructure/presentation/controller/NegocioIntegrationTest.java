package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.exception.ServicioUbicacionException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.ubicacion.LocalidadesDelPunto;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.GeocodificadorPort;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class NegocioIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String API = "/api/business";
    private static final String CONFIGURACION = """
            {"nombre":"  Mi almacén  ","rubro":"ALMACEN","comparteDatosZona":false,
             "ubicacion":{"direccion":"Calle 123","localidad":"San Justo",
                          "latitud":-34.6701234,"longitud":-58.5607894}}
            """;
    private static final String UBICACION_EN_CABA = """
            {"ubicacion":{"direccion":"Av. Corrientes 1000","localidad":"CABA",
                          "latitud":-34.6037,"longitud":-58.3816}}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // El proveedor externo nunca se consulta en los tests: el mapa reconoce "San Justo" salvo que el test diga otra cosa.
    @MockitoBean
    private GeocodificadorPort geocodificadorPort;

    private MockMvc mockMvc;
    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private SesionHttpDePrueba sesion;
    private ResultActions respuesta;
    private ResultActions respuestaDeLaConsulta;
    private UUID negocioAjeno;
    private UUID usuarioAjeno;

    @BeforeEach
    void setUp() throws Exception {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenReturn(new LocalidadesDelPunto(List.of("San Justo")));
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        usuarioId = UsuarioId.de(insertarUsuario());
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Mi negocio', ?)",
                negocioId.valor(), usuarioId.valor());
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.valor().toString());
    }

    @Test
    @DisplayName("Consultar configuración exige sesión")
    void consultarConfiguracionExigeSesion() throws Exception {
        cuandoSeConsultaLaConfiguracionSinSesion();

        entoncesResponde(401);
    }

    @Test
    @DisplayName("Actualizar configuración exige sesión")
    void actualizarConfiguracionExigeSesion() throws Exception {
        cuandoSeActualizaSinSesion();

        entoncesResponde(401);
    }

    @Test
    @DisplayName("Actualizar configuración exige CSRF")
    void actualizarConfiguracionExigeCsrf() throws Exception {
        cuandoSeActualizaSinTokenCsrf();

        entoncesResponde(403);
    }

    @Test
    @DisplayName("El negocio nuevo se consulta sin ubicación y sin exponer identificadores privados")
    void elNegocioNuevoSeConsultaSinUbicacionYSinExponerIdentificadoresPrivados() throws Exception {
        cuandoSeConsultaLaConfiguracion();

        entoncesLaConfiguracionEsLaDelNegocioNuevo();
    }

    @Test
    @DisplayName("Guarda y recupera nombre, rubro, ubicación y privacidad desde PostgreSQL")
    void guardaYRecuperaNombreRubroUbicacionYPrivacidadDesdePostgresql() throws Exception {
        cuandoSeGuardaLaConfiguracionCompleta();
        cuandoSeVuelveAConsultarLaConfiguracion();

        entoncesElGuardadoRespondeOkSinCache();
        entoncesLaConsultaTraeLaConfiguracionGuardada();
        entoncesLaBaseGuardaLaDireccion("Calle 123");
    }

    @Test
    @DisplayName("Un punto dentro de la zona de San Justo queda asignado a esa zona")
    void unPuntoDentroDeLaZonaDeSanJustoQuedaAsignadoAEsaZona() throws Exception {
        cuandoSeGuardaLaConfiguracionCompleta();
        cuandoSeVuelveAConsultarLaConfiguracion();

        entoncesElGuardadoAsignaLaZonaSanJusto();
        entoncesElNegocioTieneZonaEnLaBase();
        entoncesLaConsultaMuestraLaZonaSinSuId();
    }

    @Test
    @DisplayName("Un punto fuera de toda zona deja el negocio sin zona")
    void unPuntoFueraDeTodaZonaDejaElNegocioSinZona() throws Exception {
        dadoUnaConfiguracionGuardada();
        dadoQueElMapaReconoceLaLocalidad("CABA");

        cuandoSeGuardaUnaUbicacionEnCaba();

        entoncesLaRespuestaNoTieneZona();
        entoncesElNegocioNoTieneZonaEnLaBase();
    }

    @Test
    @DisplayName("Una localidad distinta a la del mapa responde 400 y no modifica la ubicación guardada")
    void unaLocalidadDistintaALaDelMapaResponde400YNoModificaLaUbicacionGuardada() throws Exception {
        cuandoSeGuardaConLocalidad("Ramos Mejía");

        entoncesSeRechazaLaLocalidadQueNoCoincideConElMapa();
        entoncesNoSeGuardoLaLocalidadNiElNombre();
    }

    @Test
    @DisplayName("La localidad escrita se acepta sin importar mayúsculas ni tildes")
    void laLocalidadEscritaSeAceptaSinImportarMayusculasNiTildes() throws Exception {
        cuandoSeGuardaConLocalidad("  san JUSTO ");

        entoncesSeGuardaLaLocalidadTalCualSeEscribio("san JUSTO");
    }

    @Test
    @DisplayName("Si el proveedor del mapa no responde, responde 503 y no guarda la ubicación")
    void siElProveedorDelMapaNoRespondeResponde503YNoGuardaLaUbicacion() throws Exception {
        dadoQueElProveedorDelMapaFalla();

        cuandoSeGuardaLaConfiguracionCompleta();

        entoncesResponde(503);
        entoncesLaBaseNoGuardaDireccion();
    }

    @Test
    @DisplayName("Guardar sin tocar la ubicación no consulta al proveedor del mapa")
    void guardarSinTocarLaUbicacionNoConsultaAlProveedorDelMapa() throws Exception {
        dadoUnaConfiguracionGuardada();
        dadoQueSeOlvidanLasConsultasAlMapa();

        cuandoSeCambiaSoloElNombre();

        entoncesNoSeConsultoAlProveedorDelMapa();
    }

    @Test
    @DisplayName("Un PATCH parcial conserva ubicación, rubro y privacidad ya guardados")
    void unPatchParcialConservaUbicacionRubroYPrivacidadYaGuardados() throws Exception {
        dadoUnaConfiguracionGuardada();

        cuandoSeActualiza("{\"nombre\":\"Otro nombre\"}");

        entoncesSeConservaLoQueNoSeEnvio();
    }

    @Test
    @DisplayName("Los ids enviados por el cliente nunca cambian el negocio de otro dueño")
    void losIdsEnviadosPorElClienteNuncaCambianElNegocioDeOtroDuenio() throws Exception {
        dadoUnNegocioDeOtroDuenio();

        cuandoSeEnvianLosIdsAjenos();

        entoncesSeActualizaSoloElNegocioPropio();
        entoncesElNegocioAjenoNoCambia();
    }

    @Test
    @DisplayName("No encuentra configuración para un usuario sin negocio propio")
    void noEncuentraConfiguracionParaUnUsuarioSinNegocioPropio() throws Exception {
        dadoUnaSesionDeUnUsuarioSinNegocio();

        cuandoSeConsultaLaConfiguracion();

        entoncesResponde(404);
    }

    @Test
    @DisplayName("Un usuario sin negocio propio no puede editar otro negocio")
    void unUsuarioSinNegocioPropioNoPuedeEditarOtroNegocio() throws Exception {
        dadoUnaSesionDeUnUsuarioSinNegocio();

        cuandoSeGuardaLaConfiguracionCompleta();

        entoncesResponde(404);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"nombre\":\"   \"}",
            "{\"rubro\":\"INEXISTENTE\"}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":91,\"longitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":0,\"longitud\":181}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"San Justo\",\"latitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"Calle\",\"localidad\":\"\",\"latitud\":0,\"longitud\":0}}",
            "{\"ubicacion\":{\"direccion\":\"\",\"localidad\":\"San Justo\",\"latitud\":0,\"longitud\":0}}"
    })
    @DisplayName("Los datos inválidos responden 400 y no modifican el negocio")
    void losDatosInvalidosResponden400YNoModificanElNegocio(String cuerpo) throws Exception {
        cuandoSeActualiza(cuerpo);

        entoncesResponde(400);
        entoncesElNombreDelNegocioNoCambio();
    }

    // --- dado ---

    private void dadoUnaConfiguracionGuardada() throws Exception {
        actualizar(CONFIGURACION).andExpect(status().isOk());
    }

    private void dadoQueElMapaReconoceLaLocalidad(String localidad) {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenReturn(new LocalidadesDelPunto(List.of(localidad)));
    }

    private void dadoQueElProveedorDelMapaFalla() {
        when(geocodificadorPort.resolverLocalidades(any(), any(), any()))
                .thenThrow(new ServicioUbicacionException("No pudimos verificar la localidad."));
    }

    private void dadoQueSeOlvidanLasConsultasAlMapa() {
        clearInvocations(geocodificadorPort);
    }

    private void dadoUnNegocioDeOtroDuenio() {
        negocioAjeno = UUID.randomUUID();
        usuarioAjeno = insertarUsuario();
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Ajeno', ?)",
                negocioAjeno, usuarioAjeno);
    }

    private void dadoUnaSesionDeUnUsuarioSinNegocio() throws Exception {
        sesion = SesionHttpDePrueba.autenticada(mockMvc, insertarUsuario().toString());
    }

    // --- cuando ---

    private void cuandoSeConsultaLaConfiguracionSinSesion() throws Exception {
        respuesta = mockMvc.perform(get(API));
    }

    private void cuandoSeActualizaSinSesion() throws Exception {
        respuesta = mockMvc.perform(patch(API).contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION));
    }

    private void cuandoSeActualizaSinTokenCsrf() throws Exception {
        respuesta = mockMvc.perform(patch(API).session(sesion.sesion())
                .contentType(MediaType.APPLICATION_JSON).content(CONFIGURACION));
    }

    private void cuandoSeConsultaLaConfiguracion() throws Exception {
        respuesta = mockMvc.perform(get(API).session(sesion.sesion()));
    }

    private void cuandoSeVuelveAConsultarLaConfiguracion() throws Exception {
        respuestaDeLaConsulta = mockMvc.perform(get(API).session(sesion.sesion()));
    }

    private void cuandoSeGuardaLaConfiguracionCompleta() throws Exception {
        respuesta = actualizar(CONFIGURACION);
    }

    private void cuandoSeGuardaUnaUbicacionEnCaba() throws Exception {
        respuesta = actualizar(UBICACION_EN_CABA);
    }

    private void cuandoSeGuardaConLocalidad(String localidad) throws Exception {
        respuesta = actualizar(CONFIGURACION.replace("\"localidad\":\"San Justo\"", "\"localidad\":\"" + localidad + "\""));
    }

    private void cuandoSeCambiaSoloElNombre() throws Exception {
        respuesta = actualizar(CONFIGURACION.replace("Mi almacén", "Otro nombre"));
    }

    private void cuandoSeActualiza(String cuerpo) throws Exception {
        respuesta = actualizar(cuerpo);
    }

    private void cuandoSeEnvianLosIdsAjenos() throws Exception {
        respuesta = actualizar("{\"nombre\":\"Propio\",\"negocioId\":\"" + negocioAjeno
                + "\",\"duenioId\":\"" + usuarioAjeno + "\"}");
    }

    // --- entonces ---

    private void entoncesResponde(int estado) throws Exception {
        respuesta.andExpect(status().is(estado));
    }

    private void entoncesLaConfiguracionEsLaDelNegocioNuevo() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.nombre").value("Mi negocio"))
                .andExpect(jsonPath("$.rubro").value("OTRO"))
                .andExpect(jsonPath("$.ubicacion").isEmpty())
                .andExpect(jsonPath("$.zona").isEmpty())
                .andExpect(jsonPath("$.comparteDatosZona").value(true))
                .andExpect(jsonPath("$.duenioId").doesNotExist())
                .andExpect(jsonPath("$.zonaId").doesNotExist());
    }

    private void entoncesElGuardadoRespondeOkSinCache() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }

    private void entoncesLaConsultaTraeLaConfiguracionGuardada() throws Exception {
        respuestaDeLaConsulta.andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Mi almacén"))
                .andExpect(jsonPath("$.rubro").value("ALMACEN"))
                .andExpect(jsonPath("$.ubicacion.direccion").value("Calle 123"))
                .andExpect(jsonPath("$.ubicacion.localidad").value("San Justo"))
                .andExpect(jsonPath("$.ubicacion.latitud").value(-34.670123))
                .andExpect(jsonPath("$.ubicacion.longitud").value(-58.560789))
                .andExpect(jsonPath("$.comparteDatosZona").value(false));
    }

    private void entoncesLaBaseGuardaLaDireccion(String direccion) {
        assertThat(jdbcTemplate.queryForObject("SELECT direccion FROM negocio WHERE id = ?", String.class,
                negocioId.valor())).isEqualTo(direccion);
    }

    private void entoncesLaBaseNoGuardaDireccion() {
        assertThat(jdbcTemplate.queryForObject("SELECT direccion FROM negocio WHERE id = ?", String.class,
                negocioId.valor())).isNull();
    }

    private void entoncesElGuardadoAsignaLaZonaSanJusto() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.zona").value("San Justo - UNLaM"));
    }

    private void entoncesElNegocioTieneZonaEnLaBase() {
        assertThat(jdbcTemplate.queryForObject("SELECT zona_id FROM negocio WHERE id = ?", UUID.class,
                negocioId.valor())).isNotNull();
    }

    private void entoncesElNegocioNoTieneZonaEnLaBase() {
        assertThat(jdbcTemplate.queryForObject("SELECT zona_id FROM negocio WHERE id = ?", UUID.class,
                negocioId.valor())).isNull();
    }

    private void entoncesLaConsultaMuestraLaZonaSinSuId() throws Exception {
        respuestaDeLaConsulta.andExpect(status().isOk())
                .andExpect(jsonPath("$.zona").value("San Justo - UNLaM"))
                .andExpect(jsonPath("$.zonaId").doesNotExist());
    }

    private void entoncesLaRespuestaNoTieneZona() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.zona").isEmpty());
    }

    private void entoncesSeRechazaLaLocalidadQueNoCoincideConElMapa() throws Exception {
        respuesta.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("La localidad no coincide con la del mapa (San Justo)"));
    }

    private void entoncesNoSeGuardoLaLocalidadNiElNombre() {
        assertThat(jdbcTemplate.queryForObject("SELECT localidad FROM negocio WHERE id = ?", String.class,
                negocioId.valor())).isNull();
        entoncesElNombreDelNegocioNoCambio();
    }

    private void entoncesElNombreDelNegocioNoCambio() {
        assertThat(jdbcTemplate.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class,
                negocioId.valor())).isEqualTo("Mi negocio");
    }

    private void entoncesSeGuardaLaLocalidadTalCualSeEscribio(String localidad) throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.ubicacion.localidad").value(localidad));
    }

    private void entoncesNoSeConsultoAlProveedorDelMapa() {
        verifyNoInteractions(geocodificadorPort);
    }

    private void entoncesSeConservaLoQueNoSeEnvio() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Otro nombre"))
                .andExpect(jsonPath("$.rubro").value("ALMACEN"))
                .andExpect(jsonPath("$.ubicacion.localidad").value("San Justo"))
                .andExpect(jsonPath("$.comparteDatosZona").value(false));
    }

    private void entoncesSeActualizaSoloElNegocioPropio() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.nombre").value("Propio"));
        assertThat(jdbcTemplate.queryForObject("SELECT duenio_id FROM negocio WHERE id = ?", UUID.class,
                negocioId.valor())).isEqualTo(usuarioId.valor());
    }

    private void entoncesElNegocioAjenoNoCambia() {
        assertThat(jdbcTemplate.queryForObject("SELECT nombre FROM negocio WHERE id = ?", String.class,
                negocioAjeno)).isEqualTo("Ajeno");
    }

    // --- datos ---

    private UUID insertarUsuario() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Persona')", id,
                id + "@example.test");
        return id;
    }

    private ResultActions actualizar(String cuerpo) throws Exception {
        return mockMvc.perform(patch(API).session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }
}
