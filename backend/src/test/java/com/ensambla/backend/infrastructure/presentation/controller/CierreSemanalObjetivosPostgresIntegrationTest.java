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
    private String csrfToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuarioYNegocio();
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioDe(usuarioId));
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
