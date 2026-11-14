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
    private static final String CUERPO = "{\"hizo\":\"EN_PARTE\",\"quePaso\":\"NO_TUVE_TIEMPO\","
            + "\"comentario\":\" Hablé con 1 sola vecina \"}";

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

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuarioYNegocio();
        negocioId = negocioDe(usuarioId);
        objetivoId = givenObjetivoSinMeta(negocioId);
        manualId = givenMini(negocioId, objetivoId, 1, "MANUAL");
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Un manual sin responder aparece entre los pendientes, sin métrica ni números")
    void pendientesShouldListTheManualMiniWithoutAnswer() throws Exception {
        mockMvc.perform(get(PENDIENTES).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objetivoId").value(objetivoId.toString()))
                .andExpect(jsonPath("$[0].objetivoTexto").value("Que me conozcan"))
                .andExpect(jsonPath("$[0].miniObjetivo.id").value(manualId.toString()))
                .andExpect(jsonPath("$[0].miniObjetivo.modo").value("MANUAL"))
                .andExpect(jsonPath("$[0].miniObjetivo.tipo").doesNotExist())
                .andExpect(jsonPath("$[0].miniObjetivo.meta").doesNotExist());
    }

    @Test
    @DisplayName("Los mini-objetivos medidos no son pendientes")
    void pendientesShouldNotListMeasuredMinis() throws Exception {
        givenMini(negocioId, objetivoId, 2, "MEDIDO");

        mockMvc.perform(get(PENDIENTES).session(sesion))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("Responder guarda la respuesta con su fecha, anota la decisión y saca al manual de los pendientes")
    void responderShouldSaveTheAnswerRecordTheDecisionAndClearThePending() throws Exception {
        whenResponder(manualId, CUERPO)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.miniObjetivoId").value(manualId.toString()))
                .andExpect(jsonPath("$.hizo").value("EN_PARTE"))
                .andExpect(jsonPath("$.quePaso").value("NO_TUVE_TIEMPO"))
                .andExpect(jsonPath("$.comentario").value("Hablé con 1 sola vecina"))
                .andExpect(jsonPath("$.respondidaEn").exists());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM respuesta_mini_objetivo WHERE mini_objetivo_id = ? AND negocio_id = ?",
                Integer.class, manualId, negocioId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT tipo FROM decision_objetivo WHERE mini_objetivo_id = ?", String.class, manualId))
                .containsExactly("MINI_EN_PARTE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT estado FROM mini_objetivo WHERE id = ?", String.class, manualId)).isEqualTo("EN_CURSO");
        mockMvc.perform(get(PENDIENTES).session(sesion)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("La decisión de la respuesta se ve en el historial del objetivo")
    void responderShouldShowTheDecisionInTheObjectiveHistory() throws Exception {
        whenResponder(manualId, "{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\"}").andExpect(status().isCreated());

        mockMvc.perform(get("/api/objetivos/{id}/decisiones", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].decisiones[0].tipo").value("MINI_HECHO"))
                .andExpect(jsonPath("$.semanas[0].decisiones[0].efecto").value("SIN_MEDICION"));
    }

    @Test
    @DisplayName("Responder dos veces responde 409 y la primera respuesta queda como estaba")
    void responderShouldNotOverwriteTheFirstAnswer() throws Exception {
        whenResponder(manualId, CUERPO).andExpect(status().isCreated());

        whenResponder(manualId, "{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\"}")
                .andExpect(status().isConflict());

        assertThat(jdbcTemplate.queryForObject(
                "SELECT hizo FROM respuesta_mini_objetivo WHERE mini_objetivo_id = ?", String.class, manualId))
                .isEqualTo("EN_PARTE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM decision_objetivo WHERE mini_objetivo_id = ?", Integer.class, manualId))
                .isEqualTo(1);
    }

    @Test
    @DisplayName("Un mini-objetivo medido no se responde")
    void responderShouldRejectAMeasuredMini() throws Exception {
        UUID medido = givenMini(negocioId, objetivoId, 2, "MEDIDO");

        whenResponder(medido, CUERPO).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Decir que lo hizo y que no tuvo tiempo responde 400")
    void responderShouldRejectAContradictoryAnswer() throws Exception {
        whenResponder(manualId, "{\"hizo\":\"SI\",\"quePaso\":\"NO_TUVE_TIEMPO\"}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un mini-objetivo de otro negocio responde 404 igual que uno inexistente y no se le guarda nada")
    void responderShouldReturn404ForAnotherBusinessMini() throws Exception {
        UUID otroNegocio = negocioDe(givenUsuarioYNegocio());
        UUID otroObjetivo = givenObjetivoSinMeta(otroNegocio);
        UUID ajeno = givenMini(otroNegocio, otroObjetivo, 1, "MANUAL");

        mockMvc.perform(post("/api/objetivos/{o}/mini-objetivos/{m}/respuesta", otroObjetivo, ajeno)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(CUERPO))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/objetivos/{o}/mini-objetivos/{m}/respuesta", objetivoId, UUID.randomUUID())
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json").content(CUERPO))
                .andExpect(status().isNotFound());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM respuesta_mini_objetivo", Integer.class))
                .isZero();
    }

    @Test
    @DisplayName("Los pendientes son sólo los del negocio de la sesión")
    void pendientesShouldIsolateBusinesses() throws Exception {
        UUID otroNegocio = negocioDe(givenUsuarioYNegocio());
        givenMini(otroNegocio, givenObjetivoSinMeta(otroNegocio), 1, "MANUAL");

        mockMvc.perform(get(PENDIENTES).session(sesion))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].miniObjetivo.id").value(manualId.toString()));
    }

    @Test
    @DisplayName("Responder sin CSRF responde 403 y sin sesión 401")
    void responderShouldRequireSessionAndCsrf() throws Exception {
        mockMvc.perform(post("/api/objetivos/{o}/mini-objetivos/{m}/respuesta", objetivoId, manualId)
                        .session(sesion).contentType("application/json").content(CUERPO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/objetivos/{o}/mini-objetivos/{m}/respuesta", objetivoId, manualId)
                        .contentType("application/json").content(CUERPO))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PENDIENTES)).andExpect(status().isUnauthorized());
    }

    // --- act ---
    private ResultActions whenResponder(UUID miniId, String cuerpo) throws Exception {
        return mockMvc.perform(post("/api/objetivos/{o}/mini-objetivos/{m}/respuesta", objetivoId, miniId)
                .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(cuerpo));
    }

    // --- helpers ---
    private UUID givenUsuarioYNegocio() {
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

    private UUID givenObjetivoSinMeta(UUID negocio) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, tipo, armado_con_sami) "
                + "VALUES (?, ?, 'Que me conozcan', 'HIBRIDO', true)", id, negocio);
        return id;
    }

    /** Un mini-objetivo en curso: el manual no tiene métrica ni números; el medido, los de un «ventas semanales». */
    private UUID givenMini(UUID negocio, UUID objetivo, int posicion, String modo) {
        UUID id = UUID.randomUUID();
        if (modo.equals("MANUAL")) {
            jdbcTemplate.update("INSERT INTO mini_objetivo (id, objetivo_id, negocio_id, posicion, modo, estado, "
                    + "semana_inicio, titulo, justificacion, creado_en, actualizado_en) "
                    + "VALUES (?, ?, ?, ?, 'MANUAL', 'EN_CURSO', '2026-11-09', 'Pedile a 3 clientes', 'Porque sí', "
                    + "now(), now())", id, objetivo, negocio, posicion);
        } else {
            jdbcTemplate.update("INSERT INTO mini_objetivo (id, objetivo_id, negocio_id, posicion, modo, tipo, "
                    + "meta, valor_inicial, valor_actual, aporte_estimado, estado, semana_inicio, titulo, "
                    + "justificacion, creado_en, actualizado_en) "
                    + "VALUES (?, ?, ?, ?, 'MEDIDO', 'VENTAS_SEMANALES', 110, 100, 100, 10, 'EN_CURSO', "
                    + "'2026-11-09', 'Vender más', 'Porque sí', now(), now())", id, objetivo, negocio, posicion);
        }
        return id;
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
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
