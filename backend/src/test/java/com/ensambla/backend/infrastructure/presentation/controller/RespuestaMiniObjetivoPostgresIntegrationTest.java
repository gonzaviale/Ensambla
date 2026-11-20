package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Responder un mini-objetivo manual de punta a punta con PostgreSQL: lo que se guarda y lo que queda pendiente. */
@Transactional
class RespuestaMiniObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String PENDIENTES = "/api/objetivos/respuestas-pendientes";
    private static final String RUTA_DE_RESPUESTA = "/api/objetivos/{o}/mini-objetivos/{m}/respuesta";
    private static final String CUERPO = "{\"hizo\":\"EN_PARTE\",\"quePaso\":\"NO_TUVE_TIEMPO\","
            + "\"comentario\":\" Hablé con 1 sola vecina \"}";
    private static final String CUERPO_HECHO = "{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\"}";
    private static final String CUERPO_CONTRADICTORIO = "{\"hizo\":\"SI\",\"quePaso\":\"NO_TUVE_TIEMPO\"}";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID negocioId;
    private UUID objetivoId;
    private UUID manualId;
    private UUID medidoId;
    private UUID otroObjetivoId;
    private UUID manualAjenoId;
    private ResultActions respuesta;
    private ResultActions respuestaAjena;
    private ResultActions respuestaInexistente;
    private ResultActions pendientes;
    private ResultActions decisiones;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = dadoUsuarioYNegocio();
        negocioId = negocioDe(usuarioId);
        objetivoId = dadoObjetivoSinMeta(negocioId);
        manualId = dadoManual(negocioId, objetivoId, 1);
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("Un manual sin responder aparece entre los pendientes, sin métrica ni números")
    void unManualSinResponderApareceEntreLosPendientesSinMetricaNiNumeros() throws Exception {
        cuandoSePidenLosPendientes();

        entoncesElManualApareceSinMetricaNiNumeros();
    }

    @Test
    @DisplayName("Los mini-objetivos medidos no son pendientes")
    void losMiniObjetivosMedidosNoSonPendientes() throws Exception {
        dadoUnMedido();

        cuandoSePidenLosPendientes();

        entoncesHayPendientes(1);
    }

    @Test
    @DisplayName("Responder guarda la respuesta con su fecha, anota la decisión y saca al manual de los pendientes")
    void responderGuardaLaRespuestaConSuFechaAnotaLaDecisionYSacaAlManualDeLos() throws Exception {
        cuandoSeResponde(manualId, CUERPO);
        cuandoSePidenLosPendientes();

        entoncesSeCreoLaRespuestaConSuFecha();
        entoncesSeGuardaUnaRespuestaYSuDecisionYElManualSigueEnCurso();
        entoncesHayPendientes(0);
    }

    @Test
    @DisplayName("La decisión de la respuesta se ve en el historial del objetivo")
    void laDecisionDeLaRespuestaSeVeEnElHistorialDelObjetivo() throws Exception {
        dadoElManualYaRespondidoComoHecho();

        cuandoSePideElHistorialDeDecisiones();

        entoncesElHistorialMuestraLaDecisionSinMedicion();
    }

    @Test
    @DisplayName("Responder dos veces responde 409 y la primera respuesta queda como estaba")
    void responderDosVecesResponde409YLaPrimeraRespuestaQuedaComoEstaba() throws Exception {
        dadoElManualYaRespondidoEnParte();

        cuandoSeResponde(manualId, CUERPO_HECHO);

        entoncesSeResponde409();
        entoncesLaPrimeraRespuestaQuedaComoEstaba();
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void unMiniObjetivoMedidoNoSeResponde() throws Exception {
        dadoUnMedido();

        cuandoSeResponde(medidoId, CUERPO);

        entoncesSeResponde400();
    }

    @Test
    @DisplayName("Decir que lo hizo y que no tuvo tiempo responde 400")
    void decirQueLoHizoYQueNoTuvoTiempoResponde400() throws Exception {
        cuandoSeResponde(manualId, CUERPO_CONTRADICTORIO);

        entoncesSeResponde400();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro negocio responde 404 igual que uno inexistente y no se le guarda nada")
    void unMiniObjetivoDeOtroNegocioResponde404IgualQueUnoInexistenteYNoSeLeGuarda() throws Exception {
        dadoUnManualDeOtroNegocio();

        cuandoSeRespondeAUnoAjenoYAUnoInexistente();

        entoncesAmbosRespondenNotFound();
        entoncesNoSeGuardoNingunaRespuesta();
    }

    @Test
    @DisplayName("Los pendientes son sólo los del negocio de la sesión")
    void losPendientesSonSoloLosDelNegocioDeLaSesion() throws Exception {
        dadoUnManualDeOtroNegocio();

        cuandoSePidenLosPendientes();

        entoncesSoloEstaElManualPropio();
    }

    @Test
    @DisplayName("Responder sin CSRF responde 403 y sin sesión 401")
    void responderSinCsrfResponde403YSinSesion401() throws Exception {
        entoncesResponderSinCsrfResponde403();
        entoncesResponderSinSesionResponde401();
        entoncesPendientesSinSesionResponde401();
    }

    // --- arrange ---
    private void dadoUnMedido() {
        medidoId = dadoMedido(negocioId, objetivoId, 2);
    }

    private void dadoElManualYaRespondidoComoHecho() throws Exception {
        cuandoSeResponde(manualId, CUERPO_HECHO);
        respuesta.andExpect(status().isCreated());
    }

    private void dadoElManualYaRespondidoEnParte() throws Exception {
        cuandoSeResponde(manualId, CUERPO);
        respuesta.andExpect(status().isCreated());
    }

    private void dadoUnManualDeOtroNegocio() {
        UUID otroNegocio = negocioDe(dadoUsuarioYNegocio());
        otroObjetivoId = dadoObjetivoSinMeta(otroNegocio);
        manualAjenoId = dadoManual(otroNegocio, otroObjetivoId, 1);
    }

    // --- act ---
    private void cuandoSeResponde(UUID miniId, String cuerpo) throws Exception {
        respuesta = mockMvc.perform(post(RUTA_DE_RESPUESTA, objetivoId, miniId)
                .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeRespondeAUnoAjenoYAUnoInexistente() throws Exception {
        respuestaAjena = mockMvc.perform(post(RUTA_DE_RESPUESTA, otroObjetivoId, manualAjenoId)
                .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(CUERPO));
        respuestaInexistente = mockMvc.perform(post(RUTA_DE_RESPUESTA, objetivoId, UUID.randomUUID())
                .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(CUERPO));
    }

    private void cuandoSePidenLosPendientes() throws Exception {
        pendientes = mockMvc.perform(get(PENDIENTES).session(sesion));
    }

    private void cuandoSePideElHistorialDeDecisiones() throws Exception {
        decisiones = mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion));
    }

    // --- assert ---
    private void entoncesElManualApareceSinMetricaNiNumeros() throws Exception {
        pendientes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objetivoId").value(objetivoId.toString()))
                .andExpect(jsonPath("$[0].objetivoTexto").value("Que me conozcan"))
                .andExpect(jsonPath("$[0].miniObjetivo.id").value(manualId.toString()))
                .andExpect(jsonPath("$[0].miniObjetivo.modo").value("MANUAL"))
                .andExpect(jsonPath("$[0].miniObjetivo.tipo").doesNotExist())
                .andExpect(jsonPath("$[0].miniObjetivo.meta").doesNotExist());
    }

    private void entoncesHayPendientes(int cantidad) throws Exception {
        pendientes.andExpect(jsonPath("$.length()").value(cantidad));
    }

    private void entoncesSeCreoLaRespuestaConSuFecha() throws Exception {
        respuesta
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.miniObjetivoId").value(manualId.toString()))
                .andExpect(jsonPath("$.hizo").value("EN_PARTE"))
                .andExpect(jsonPath("$.quePaso").value("NO_TUVE_TIEMPO"))
                .andExpect(jsonPath("$.comentario").value("Hablé con 1 sola vecina"))
                .andExpect(jsonPath("$.respondidaEn").exists());
    }

    private void entoncesSeGuardaUnaRespuestaYSuDecisionYElManualSigueEnCurso() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM respuesta_mini_objetivo WHERE mini_objetivo_id = ? AND negocio_id = ?",
                Integer.class, manualId, negocioId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT tipo FROM decision_objetivo WHERE mini_objetivo_id = ?", String.class, manualId))
                .containsExactly("MINI_EN_PARTE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT estado FROM mini_objetivo WHERE id = ?", String.class, manualId)).isEqualTo("EN_CURSO");
    }

    private void entoncesElHistorialMuestraLaDecisionSinMedicion() throws Exception {
        decisiones
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("MINI_HECHO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    private void entoncesSeResponde409() throws Exception {
        respuesta.andExpect(status().isConflict());
    }

    private void entoncesSeResponde400() throws Exception {
        respuesta.andExpect(status().isBadRequest());
    }

    private void entoncesLaPrimeraRespuestaQuedaComoEstaba() {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT hizo FROM respuesta_mini_objetivo WHERE mini_objetivo_id = ?", String.class, manualId))
                .isEqualTo("EN_PARTE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE mini_objetivo_id = ?", Integer.class, manualId))
                .isEqualTo(1);
    }

    private void entoncesAmbosRespondenNotFound() throws Exception {
        respuestaAjena.andExpect(status().isNotFound());
        respuestaInexistente.andExpect(status().isNotFound());
    }

    private void entoncesNoSeGuardoNingunaRespuesta() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM respuesta_mini_objetivo", Integer.class))
                .isZero();
    }

    private void entoncesSoloEstaElManualPropio() throws Exception {
        pendientes
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].miniObjetivo.id").value(manualId.toString()));
    }

    private void entoncesResponderSinCsrfResponde403() throws Exception {
        mockMvc.perform(post(RUTA_DE_RESPUESTA, objetivoId, manualId)
                        .session(sesion).contentType("application/json").content(CUERPO))
                .andExpect(status().isForbidden());
    }

    private void entoncesResponderSinSesionResponde401() throws Exception {
        mockMvc.perform(post(RUTA_DE_RESPUESTA, objetivoId, manualId)
                        .contentType("application/json").content(CUERPO))
                .andExpect(status().isUnauthorized());
    }

    private void entoncesPendientesSinSesionResponde401() throws Exception {
        mockMvc.perform(get(PENDIENTES)).andExpect(status().isUnauthorized());
    }

    // --- helpers ---
    private UUID dadoUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Respuestas HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Respuestas HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }

    private UUID negocioDe(UUID usuarioId) {
        return jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
    }

    private UUID dadoObjetivoSinMeta(UUID negocio) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, tipo, armado_con_sami) "
                + "VALUES (?, ?, 'Que me conozcan', 'HIBRIDO', true)", id, negocio);
        return id;
    }

    /** Un manual en curso: no tiene métrica ni números. */
    private UUID dadoManual(UUID negocio, UUID objetivo, int posicion) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO mini_objetivo (id, objetivo_id, negocio_id, posicion, modo, estado, "
                + "semana_inicio, titulo, justificacion, creado_en, actualizado_en) "
                + "VALUES (?, ?, ?, ?, 'MANUAL', 'EN_CURSO', '2026-11-09', 'Pedile a 3 clientes', 'Porque sí', "
                + "now(), now())", id, objetivo, negocio, posicion);
        return id;
    }

    /** Un medido en curso, con los números de un «ventas semanales». */
    private UUID dadoMedido(UUID negocio, UUID objetivo, int posicion) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO mini_objetivo (id, objetivo_id, negocio_id, posicion, modo, tipo, "
                + "meta, valor_inicial, valor_actual, aporte_estimado, estado, semana_inicio, titulo, "
                + "justificacion, creado_en, actualizado_en) "
                + "VALUES (?, ?, ?, ?, 'MEDIDO', 'VENTAS_SEMANALES', 110, 100, 100, 10, 'EN_CURSO', "
                + "'2026-11-09', 'Vender más', 'Porque sí', now(), now())", id, objetivo, negocio, posicion);
        return id;
    }

    private MockHttpSession dadoSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        return session;
    }
}
