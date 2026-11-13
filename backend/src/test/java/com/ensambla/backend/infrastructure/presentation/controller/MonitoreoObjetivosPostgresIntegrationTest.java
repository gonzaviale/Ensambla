package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.useCase.objetivo.monitorearObjetivos.MonitorearObjetivos;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.support.AlmacenDeObjetivosDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
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
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID negocioId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuarioYNegocio();
        negocioId = negocioDe(usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Una pasada guarda una foto del objetivo y una de cada mini-objetivo en curso")
    void monitorearShouldSaveOnePhotoPerObjetivoAndMiniObjetivo() throws Exception {
        UUID objetivoId = givenObjetivoConMinis();

        monitorearObjetivos.execute();

        assertThat(fotosDe(objetivoId)).isEqualTo(4);
        assertThat(fotosDelObjetivoMayor(objetivoId)).isEqualTo(1);
    }

    @Test
    @DisplayName("Correr dos veces el mismo día deja una sola foto por objetivo y por mini-objetivo")
    void monitorearShouldKeepOnePhotoPerSubjectAndDay() throws Exception {
        UUID objetivoId = givenObjetivoConMinis();

        monitorearObjetivos.execute();
        monitorearObjetivos.execute();

        assertThat(fotosDe(objetivoId)).isEqualTo(4);
    }

    @Test
    @DisplayName("Mide el valor actual de cada mini-objetivo con los cuadernos de hoy")
    void monitorearShouldRefreshTheCurrentValueOfEachMiniObjetivo() throws Exception {
        UUID objetivoId = givenObjetivoConMinis();
        jdbcTemplate.update("UPDATE mini_objetivo SET valor_actual = 1 WHERE objetivo_id = ?", objetivoId);

        monitorearObjetivos.execute();

        List<BigDecimal> valores = jdbcTemplate.queryForList(
                "SELECT valor_actual FROM mini_objetivo WHERE objetivo_id = ?", BigDecimal.class, objetivoId);
        assertThat(valores).hasSize(3).noneMatch(valor -> valor.compareTo(BigDecimal.ONE) == 0);
    }

    @Test
    @DisplayName("No crea avisos ni toca la tabla de avisos")
    void monitorearShouldNotCreateNotices() throws Exception {
        givenObjetivoConMinis();
        int avisosAntes = contar("aviso");

        monitorearObjetivos.execute();

        assertThat(contar("aviso")).isEqualTo(avisosAntes);
    }

    @Test
    @DisplayName("Marca como logrado el mini-objetivo cuya meta ya se cumplió, sin esperar al domingo")
    void monitorearShouldMarkAsAchievedTheMiniObjetivosWhoseGoalWasMet() throws Exception {
        UUID objetivoId = givenObjetivoConMinis();
        jdbcTemplate.update("UPDATE mini_objetivo SET meta = 0 WHERE objetivo_id = ?", objetivoId);

        monitorearObjetivos.execute();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ? AND estado = 'LOGRADO'",
                Integer.class, objetivoId)).isEqualTo(3);
    }

    @Test
    @DisplayName("Un objetivo anterior, sin mini-objetivos, recibe los suyos en la primera pasada")
    void monitorearShouldProposeMiniObjetivosToAnObjetivoWithoutThem() {
        UUID objetivoId = givenObjetivoSinMinis(500000);

        monitorearObjetivos.execute();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ? AND estado = 'EN_CURSO' "
                        + "AND texto_con_ia = false",
                Integer.class, objetivoId)).isEqualTo(3);
    }

    @Test
    @DisplayName("Marca el objetivo como alcanzado cuando el resultado llega a la meta y no le propone nada")
    void monitorearShouldMarkTheObjetivoAsAchieved() {
        UUID objetivoId = givenObjetivoSinMinis(1000);

        monitorearObjetivos.execute();

        assertThat(jdbcTemplate.queryForObject(
                "SELECT alcanzado_en IS NOT NULL FROM objetivo WHERE id = ?", Boolean.class, objetivoId)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM mini_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId)).isZero();
    }

    @Test
    @DisplayName("El progreso trae la serie del objetivo con la semana medida y la de cada mini-objetivo")
    void progresoShouldReturnTheMeasuredSeries() throws Exception {
        UUID objetivoId = givenObjetivoConMinis();
        monitorearObjetivos.execute();

        mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").value(500000.0))
                .andExpect(jsonPath("$.semanas[?(@.estimado == false)].semana").isNotEmpty())
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.minis[0].mini.estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.minis[0].semanas.length()").value(1))
                .andExpect(jsonPath("$.minis[0].semanas[0].estimado").value(false));
    }

    @Test
    @DisplayName("El progreso de un objetivo de otro negocio responde 404 igual que uno inexistente")
    void progresoShouldReturn404ForAnotherBusinessObjetivo() throws Exception {
        UUID ajeno = givenObjetivoDeOtroNegocio();

        mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", ajeno).session(sesion))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("El progreso sin sesión responde 401")
    void progresoShouldRequireSession() throws Exception {
        mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // --- arrange ---
    private UUID givenObjetivoConMinis() throws Exception {
        String body = mockMvc.perform(post(OBJETIVOS).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"texto\":\"Quiero ganar más\",\"meta\":500000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private UUID givenObjetivoSinMinis(int meta) {
        UUID objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Anterior', ?)",
                objetivoId, negocioId, meta);
        return objetivoId;
    }

    private UUID givenUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Monitoreo HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Monitoreo HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }

    private UUID givenObjetivoDeOtroNegocio() {
        UUID otroNegocio = negocioDe(givenUsuarioYNegocio());
        UUID objetivoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Ajeno', 400000)",
                objetivoId, otroNegocio);
        return objetivoId;
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
    private int fotosDe(UUID objetivoId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM progreso_objetivo WHERE objetivo_id = ?", Integer.class, objetivoId);
    }

    private int fotosDelObjetivoMayor(UUID objetivoId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM progreso_objetivo WHERE objetivo_id = ? AND mini_objetivo_id IS NULL",
                Integer.class, objetivoId);
    }

    private int contar(String tabla) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
    }

    // --- helpers ---
    private UUID negocioDe(UUID usuarioId) {
        return jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
    }
}
