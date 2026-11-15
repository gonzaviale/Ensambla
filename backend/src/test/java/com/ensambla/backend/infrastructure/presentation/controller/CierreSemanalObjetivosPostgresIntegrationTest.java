package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.objetivo.cerrarSemanasObjetivos.CerrarSemanasObjetivos;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.support.AlmacenDeObjetivosDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * El cierre del domingo de punta a punta con PostgreSQL: cierra cada semana una sola vez y no crea avisos.
 */
@Transactional
class CierreSemanalObjetivosPostgresIntegrationTest extends PostgresRepositoryTestSupport {

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
    private CerrarSemanasObjetivos cerrarSemanasObjetivos;

    @PersistenceContext
    private EntityManager entityManager;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private UUID negocioId;
    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuarioYNegocio();
        negocioId = negocioDe(usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Cierra las semanas pasadas del objetivo, una sola vez aunque corra dos veces")
    void cerrarShouldCloseEachPastWeekOnlyOnce() throws Exception {
        UUID objetivoId = givenObjetivoDeHaceDosSemanas();

        cerrarSemanasObjetivos.execute();
        int cerradas = resumenesDe(objetivoId);
        cerrarSemanasObjetivos.execute();

        assertThat(cerradas).isBetween(2, 3);
        assertThat(resumenesDe(objetivoId)).isEqualTo(cerradas);
    }

    @Test
    @DisplayName("Un objetivo creado esta semana no tiene nada que cerrar todavía")
    void cerrarShouldNotCloseTheCurrentWeek() throws Exception {
        UUID objetivoId = givenObjetivo();

        cerrarSemanasObjetivos.execute();

        assertThat(resumenesDe(objetivoId)).isZero();
    }

    @Test
    @DisplayName("El cierre no crea avisos")
    void cerrarShouldNotCreateNotices() throws Exception {
        givenObjetivoDeHaceDosSemanas();
        int avisosAntes = contar("aviso");

        cerrarSemanasObjetivos.execute();

        assertThat(contar("aviso")).isEqualTo(avisosAntes);
    }

    @Test
    @DisplayName("Las semanas del objetivo vienen de la más nueva a la más vieja con su resumen")
    void semanasShouldReturnTheClosedWeeks() throws Exception {
        UUID objetivoId = givenObjetivoDeHaceDosSemanas();
        cerrarSemanasObjetivos.execute();

        String body = mockMvc.perform(get(OBJETIVOS + "/{id}/semanas", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.semanas[0].queFunciono").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        List<String> semanas = JsonPath.read(body, "$.semanas[*].semana");
        assertThat(semanas).hasSize(resumenesDe(objetivoId)).isSortedAccordingTo((a, b) -> b.compareTo(a));
        assertThat((String) JsonPath.read(body, "$.ultimoCierre")).isEqualTo(semanas.getFirst());
    }

    @Test
    @DisplayName("El cierre renueva el manual respondido, deja sin renovar el que no y lo cuenta en el resumen")
    void cerrarShouldRenewTheAnsweredManualAndKeepTheOtherOne() throws Exception {
        UUID objetivoId = givenObjetivoSinMetaDeHaceDosSemanas();
        UUID respondido = givenManual(objetivoId, 1, "Pedile a 3 clientes que te recomienden");
        UUID sinRespuesta = givenManual(objetivoId, 2, "Poné un cartel en la vidriera");
        jdbcTemplate.update("INSERT INTO respuesta_mini_objetivo (negocio_id, objetivo_id, mini_objetivo_id, hizo, "
                + "que_paso, semana_inicio, respondida_en) VALUES (?, ?, ?, 'SI', 'FUNCIONO', current_date, now())",
                negocioId, objetivoId, respondido);

        cerrarSemanasObjetivos.execute();

        assertThat(estadoDe(respondido)).isEqualTo("LOGRADO");
        assertThat(jdbcTemplate.queryForObject("SELECT reemplazado_por FROM mini_objetivo WHERE id = ?", UUID.class,
                respondido)).isNotNull();
        assertThat(estadoDe(sinRespuesta)).isEqualTo("EN_CURSO");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ? "
                + "AND posicion = 1 AND estado = 'EN_CURSO' AND modo = 'MANUAL' AND id <> ?", Integer.class,
                objetivoId, respondido)).isEqualTo(1);
        String semanas = mockMvc.perform(get(OBJETIVOS + "/{id}/semanas", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(JsonPath.<List<String>>read(semanas, "$.semanas[*].reajustes[*].tipo")).containsExactly("SUBIR_META");
        assertThat(JsonPath.<List<Object>>read(semanas, "$.semanas[*].reajustes[*].de.meta")).containsOnlyNulls();
        assertThat(JsonPath.<List<String>>read(semanas, "$.semanas[*].manuales[*].titulo"))
                .contains("Pedile a 3 clientes que te recomienden", "Poné un cartel en la vidriera");
        assertThat(JsonPath.<List<String>>read(semanas, "$.semanas[*].manuales[?(@.hizo == 'SI')].quePaso"))
                .containsExactly("FUNCIONO");
        assertThat(JsonPath.<List<String>>read(semanas, "$.semanas[*].queFunciono"))
                .anyMatch(texto -> texto.contains("No me contaste cómo te fue con «Poné un cartel en la vidriera»"));
    }

    @Test
    @DisplayName("Después del cierre Sami pregunta primero por el manual que sigue sin respuesta")
    void pendientesShouldAskFirstForTheManualThatWasNeverAnswered() throws Exception {
        UUID objetivoId = givenObjetivoSinMetaDeHaceDosSemanas();
        UUID respondido = givenManual(objetivoId, 1, "Pedile a 3 clientes que te recomienden");
        givenManual(objetivoId, 2, "Poné un cartel en la vidriera");
        jdbcTemplate.update("INSERT INTO respuesta_mini_objetivo (negocio_id, objetivo_id, mini_objetivo_id, hizo, "
                + "que_paso, semana_inicio, respondida_en) VALUES (?, ?, ?, 'EN_PARTE', 'NO_TUVE_TIEMPO', "
                + "current_date, now())", negocioId, objetivoId, respondido);

        cerrarSemanasObjetivos.execute();

        mockMvc.perform(get(OBJETIVOS + "/respuestas-pendientes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].miniObjetivo.titulo").value("Poné un cartel en la vidriera"))
                .andExpect(jsonPath("$[1].miniObjetivo.titulo").value(startsWith("Lo que faltó: ")));
    }

    @Test
    @DisplayName("Las semanas de un objetivo inexistente o ajeno responden 404")
    void semanasShouldReturn404ForAnUnknownObjetivo() throws Exception {
        mockMvc.perform(get(OBJETIVOS + "/{id}/semanas", UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Las semanas sin sesión responden 401")
    void semanasShouldRequireSession() throws Exception {
        mockMvc.perform(get(OBJETIVOS + "/{id}/semanas", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // --- arrange ---
    private UUID givenObjetivo() throws Exception {
        String body = mockMvc.perform(post(OBJETIVOS).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"texto\":\"Quiero ganar más\",\"meta\":500000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID givenObjetivoDeHaceDosSemanas() throws Exception {
        UUID objetivoId = givenObjetivo();
        entityManager.flush();
        jdbcTemplate.update("UPDATE objetivo SET creado_en = now() - interval '15 days' WHERE id = ?", objetivoId);
        entityManager.clear();
        return objetivoId;
    }

    /** Un objetivo armado con Sami, sin meta, creado hace 15 días (tiene semanas por cerrar). */
    private UUID givenObjetivoSinMetaDeHaceDosSemanas() {
        UUID objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, tipo, armado_con_sami, creado_en) "
                + "VALUES (?, ?, 'Que me conozcan', 'SIN_DATOS', true, now() - interval '15 days')",
                objetivoId, negocioId);
        return objetivoId;
    }

    /** Un mini-objetivo manual en curso que empezó antes de la primera semana por cerrar. */
    private UUID givenManual(UUID objetivoId, int posicion, String titulo) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO mini_objetivo (id, objetivo_id, negocio_id, posicion, modo, estado, "
                + "semana_inicio, titulo, justificacion, creado_en, actualizado_en) "
                + "VALUES (?, ?, ?, ?, 'MANUAL', 'EN_CURSO', current_date - 40, ?, 'Porque sí', "
                + "now() - interval '15 days', now() - interval '15 days')", id, objetivoId, negocioId, posicion, titulo);
        return id;
    }

    private String estadoDe(UUID miniObjetivoId) {
        return jdbcTemplate.queryForObject("SELECT estado FROM mini_objetivo WHERE id = ?", String.class,
                miniObjetivoId);
    }

    private UUID givenUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Cierre HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Cierre HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }

    private MockHttpSession givenSesion(UUID usuarioId) throws Exception {
        MockHttpSession session = new MockHttpSession();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        String body = mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString();
        csrfToken = JsonMapper.builder().build().readValue(body, CsrfResponse.class).token();
        return session;
    }

    // --- assert ---
    private int resumenesDe(UUID objetivoId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM resumen_semanal_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId);
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    // --- helpers ---
    private UUID negocioDe(UUID usuarioId) {
        return jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
    }
}
