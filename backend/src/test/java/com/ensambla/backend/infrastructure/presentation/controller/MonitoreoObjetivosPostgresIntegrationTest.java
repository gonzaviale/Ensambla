package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import com.ensambla.backend.support.AlmacenDeObjetivosDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SesionHttpDePrueba;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El monitoreo silencioso de punta a punta con PostgreSQL: mide, guarda una foto por día y no crea avisos.
 */
@Transactional
class MonitoreoObjetivosPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String OBJETIVOS = "/api/objetivos";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SistemaRepository sistemaRepository;

    @Autowired
    private MonitorearObjetivos monitorearObjetivos;

    private MockMvc mockMvc;
    private SesionHttpDePrueba sesion;
    private UUID negocioId;
    private UUID objetivoId;
    private ResultActions respuesta;
    private int avisosAntes;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = insertarUsuarioYNegocio();
        negocioId = negocioDe(usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Una pasada guarda una foto del objetivo y una de cada mini-objetivo en curso")
    void unaPasadaGuardaUnaFotoDelObjetivoYUnaDeCadaMiniObjetivoEnCurso() throws Exception {
        dadoUnObjetivoConMinis();

        cuandoSeMonitoreaUnaVez();

        entoncesHayCuatroFotosYUnaDelObjetivoMayor();
    }

    @Test
    @DisplayName("Correr dos veces el mismo día deja una sola foto por objetivo y por mini-objetivo")
    void correrDosVecesElMismoDiaDejaUnaSolaFotoPorObjetivoYPorMiniObjetivo() throws Exception {
        dadoUnObjetivoConMinis();

        cuandoSeMonitoreaDosVeces();

        entoncesHayCuatroFotos();
    }

    @Test
    @DisplayName("Mide el valor actual de cada mini-objetivo con los cuadernos de hoy")
    void mideElValorActualDeCadaMiniObjetivoConLosCuadernosDeHoy() throws Exception {
        dadoUnObjetivoConMinis();
        dadoQueLosMinisValenUno();

        cuandoSeMonitoreaUnaVez();

        entoncesLosMinisYaNoValenUno();
    }

    @Test
    @DisplayName("No crea avisos ni toca la tabla de avisos")
    void noCreaAvisosNiTocaLaTablaDeAvisos() throws Exception {
        dadoUnObjetivoConMinis();
        dadoLaCantidadActualDeAvisos();

        cuandoSeMonitoreaUnaVez();

        entoncesLaCantidadDeAvisosNoCambio();
    }

    @Test
    @DisplayName("Marca como logrado el mini-objetivo cuya meta ya se cumplió, sin esperar al domingo")
    void marcaComoLogradoElMiniObjetivoCuyaMetaYaSeCumplioSinEsperarAlDomingo() throws Exception {
        dadoUnObjetivoConMinis();
        dadoQueLaMetaDeLosMinisYaSeCumplio();

        cuandoSeMonitoreaUnaVez();

        entoncesLosTresMinisQuedanLogrados();
    }

    @Test
    @DisplayName("Un objetivo anterior, sin mini-objetivos, recibe los suyos en la primera pasada")
    void unObjetivoAnteriorSinMiniObjetivosRecibeLosSuyosEnLaPrimeraPasada() {
        dadoUnObjetivoSinMinisConMeta(500000);

        cuandoSeMonitoreaUnaVez();

        entoncesRecibeTresMinisEnCursoSinTextoDeIa();
    }

    @Test
    @DisplayName("Marca el objetivo como alcanzado cuando el resultado llega a la meta y no le propone nada")
    void marcaElObjetivoComoAlcanzadoCuandoElResultadoLlegaALaMetaYNoLePropone() {
        dadoUnObjetivoSinMinisConMeta(1000);

        cuandoSeMonitoreaUnaVez();

        entoncesElObjetivoQuedaAlcanzadoYSinMinis();
    }

    @Test
    @DisplayName("El progreso trae la serie del objetivo con la semana medida y la de cada mini-objetivo")
    void elProgresoTraeLaSerieDelObjetivoConLaSemanaMedidaYLaDeCadaMiniObjetivo() throws Exception {
        dadoUnObjetivoConMinis();
        dadoQueSeMonitoreo();

        cuandoSePideElProgresoDelObjetivo();

        entoncesElProgresoTraeLaSerieMedida();
    }

    @Test
    @DisplayName("El progreso de un objetivo de otro negocio responde 404")
    void elProgresoDeUnObjetivoDeOtroNegocioResponde404() throws Exception {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSePideElProgresoDelObjetivo();

        entoncesResponde404();
    }

    @Test
    @DisplayName("El progreso de un objetivo inexistente responde 404")
    void elProgresoDeUnObjetivoInexistenteResponde404() throws Exception {
        cuandoSePideElProgresoDeUnObjetivoInexistente();

        entoncesResponde404();
    }

    @Test
    @DisplayName("El progreso sin sesión responde 401")
    void elProgresoSinSesionResponde401() throws Exception {
        cuandoSePideElProgresoSinSesion();

        entoncesResponde401();
    }

    // --- dado ---

    private void dadoUnObjetivoConMinis() throws Exception {
        String cuerpo = mockMvc.perform(post(OBJETIVOS).session(sesion.sesion())
                        .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                        .contentType("application/json")
                        .content("{\"texto\":\"Quiero ganar más\",\"meta\":500000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        objetivoId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));
    }

    private void dadoUnObjetivoSinMinisConMeta(int meta) {
        objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Anterior', ?)",
                objetivoId, negocioId, meta);
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        UUID otroNegocio = negocioDe(insertarUsuarioYNegocio());
        objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Ajeno', 400000)",
                objetivoId, otroNegocio);
    }

    private void dadoQueLosMinisValenUno() {
        jdbcTemplate.update("UPDATE mini_objetivo SET valor_actual = 1 WHERE objetivo_id = ?", objetivoId);
    }

    private void dadoQueLaMetaDeLosMinisYaSeCumplio() {
        jdbcTemplate.update("UPDATE mini_objetivo SET meta = 0 WHERE objetivo_id = ?", objetivoId);
    }

    private void dadoLaCantidadActualDeAvisos() {
        avisosAntes = contar("aviso");
    }

    private void dadoQueSeMonitoreo() {
        monitorearObjetivos.execute();
    }

    // --- cuando ---

    private void cuandoSeMonitoreaUnaVez() {
        monitorearObjetivos.execute();
    }

    private void cuandoSeMonitoreaDosVeces() {
        monitorearObjetivos.execute();
        monitorearObjetivos.execute();
    }

    private void cuandoSePideElProgresoDelObjetivo() throws Exception {
        respuesta = mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", objetivoId).session(sesion.sesion()));
    }

    private void cuandoSePideElProgresoDeUnObjetivoInexistente() throws Exception {
        respuesta = mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", UUID.randomUUID()).session(sesion.sesion()));
    }

    private void cuandoSePideElProgresoSinSesion() throws Exception {
        respuesta = mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", UUID.randomUUID()));
    }

    // --- entonces ---

    private void entoncesHayCuatroFotosYUnaDelObjetivoMayor() {
        entoncesHayCuatroFotos();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM progreso_objetivo WHERE objetivo_id = ? AND mini_objetivo_id IS NULL",
                Integer.class, objetivoId)).isEqualTo(1);
    }

    private void entoncesHayCuatroFotos() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM progreso_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId))
                .isEqualTo(4);
    }

    private void entoncesLosMinisYaNoValenUno() {
        List<BigDecimal> valores = jdbcTemplate.queryForList(
                "SELECT valor_actual FROM mini_objetivo WHERE objetivo_id = ?", BigDecimal.class, objetivoId);
        assertThat(valores).hasSize(3).noneMatch(valor -> valor.compareTo(BigDecimal.ONE) == 0);
    }

    private void entoncesLaCantidadDeAvisosNoCambio() {
        assertThat(contar("aviso")).isEqualTo(avisosAntes);
    }

    private void entoncesLosTresMinisQuedanLogrados() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ? AND estado = 'LOGRADO'",
                Integer.class, objetivoId)).isEqualTo(3);
    }

    private void entoncesRecibeTresMinisEnCursoSinTextoDeIa() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ? AND estado = 'EN_CURSO' "
                        + "AND texto_con_ia = false",
                Integer.class, objetivoId)).isEqualTo(3);
    }

    private void entoncesElObjetivoQuedaAlcanzadoYSinMinis() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT alcanzado_en IS NOT NULL FROM objetivo WHERE id = ?", Boolean.class, objetivoId)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId)).isZero();
    }

    private void entoncesElProgresoTraeLaSerieMedida() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").value(500000.0))
                .andExpect(jsonPath("$.semanas[?(@.estimado == false)].semana").isNotEmpty())
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.minis[0].mini.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.minis[0].semanas.length()").value(1))
                .andExpect(jsonPath("$.minis[0].semanas[0].estimado").value(false));
    }

    private void entoncesResponde404() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesResponde401() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    // --- datos ---

    private UUID insertarUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Monitoreo HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Monitoreo HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }

    private UUID negocioDe(UUID usuarioId) {
        return jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }
}
