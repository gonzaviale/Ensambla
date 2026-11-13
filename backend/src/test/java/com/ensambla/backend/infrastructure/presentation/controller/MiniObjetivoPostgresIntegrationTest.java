package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Los mini-objetivos de punta a punta con PostgreSQL: la propuesta con datos reales y los cambios de la persona. */
@Transactional
class MiniObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

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

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID negocioId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = givenUsuarioYNegocio();
        negocioId = jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        sesion = givenSesion(usuarioId);
    }

    @Test
    @DisplayName("Un objetivo nuevo trae 3 mini-objetivos distintos, con meta y valor actual calculados con sus datos")
    void crearShouldProposeThreeDistinctMiniObjetivosWithRealNumbers() throws Exception {
        String objetivoId = whenCrearObjetivo();

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.minis[0].posicion").value(1))
                .andExpect(jsonPath("$.minis[1].posicion").value(2))
                .andExpect(jsonPath("$.minis[2].posicion").value(3))
                .andExpect(jsonPath("$.minis[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.minis[0].textoConIa").value(false))
                .andExpect(jsonPath("$.historial.length()").value(0))
                .andExpect(jsonPath("$.faltanDatos").doesNotExist());
        List<String> tipos = JsonPath.read(whenListar(objetivoId), "$.minis[*].tipo");
        assertThat(tipos).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("Un objetivo cuya meta ya se alcanza nace alcanzado y sin mini-objetivos")
    void crearShouldReportAReachedObjetivoWithoutMiniObjetivos() throws Exception {
        String objetivoId = whenCrearObjetivo(1000);

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(0))
                .andExpect(jsonPath("$.faltanDatos").value("OBJETIVO_ALCANZADO"));
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde 404 igual que uno inexistente")
    void listarShouldReturn404ForAnotherBusinessObjetivo() throws Exception {
        UUID ajeno = givenObjetivoDeOtroNegocio();

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", ajeno).session(sesion))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Sin sesión responde 401")
    void listarShouldRequireSession() throws Exception {
        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cambiar un mini-objetivo por una alternativa lo deja en su lugar y el viejo queda en el historial")
    void reemplazarShouldKeepThePlaceAndMoveTheOldOneToHistory() throws Exception {
        String objetivoId = whenCrearObjetivo();
        String viejo = JsonPath.read(whenListar(objetivoId), "$.minis[0].id");
        String candidatos = mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos/{mini}/candidatos", objetivoId, viejo)
                        .session(sesion))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> elegido = new LinkedHashMap<>();
        elegido.put("tipo", JsonPath.read(candidatos, "$[0].tipo"));
        elegido.put("registroId", JsonPath.read(candidatos, "$[0].registroId"));
        elegido.put("referencia", JsonPath.read(candidatos, "$[0].referencia"));

        mockMvc.perform(post(OBJETIVOS + "/{id}/mini-objetivos/{mini}/reemplazo", objetivoId, viejo)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content(JsonMapper.builder().build().writeValueAsString(elegido)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posicion").value(1))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].id").value(viejo))
                .andExpect(jsonPath("$.historial[0].estado").value("REEMPLAZADO"));
    }

    @Test
    @DisplayName("Elegir algo que no es una alternativa responde 400 y no cambia nada")
    void reemplazarShouldRejectAnUnavailableChoice() throws Exception {
        String objetivoId = whenCrearObjetivo();
        String mini = JsonPath.read(whenListar(objetivoId), "$.minis[0].id");

        mockMvc.perform(post(OBJETIVOS + "/{id}/mini-objetivos/{mini}/reemplazo", objetivoId, mini)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"tipo\":\"RECORTAR_GASTO\",\"referencia\":\"Una categoría que no existe\"}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.historial.length()").value(0));
    }

    @Test
    @DisplayName("Quitar un mini-objetivo responde 204, lo deja en el historial y libera su lugar")
    void abandonarShouldMoveItToHistory() throws Exception {
        String objetivoId = whenCrearObjetivo();
        String mini = JsonPath.read(whenListar(objetivoId), "$.minis[2].id");

        mockMvc.perform(delete(OBJETIVOS + "/{id}/mini-objetivos/{mini}", objetivoId, mini)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(jsonPath("$.minis.length()").value(2))
                .andExpect(jsonPath("$.historial[0].estado").value("ABANDONADO"))
                .andExpect(jsonPath("$.faltanDatos").value("POCOS_DATOS"));
    }

    @Test
    @DisplayName("Quitar un mini-objetivo de otro negocio responde 404 y lo conserva")
    void abandonarShouldNotRemoveAnotherBusinessMiniObjetivo() throws Exception {
        UUID ajeno = givenObjetivoDeOtroNegocio();
        UUID miniAjeno = UUID.randomUUID();

        mockMvc.perform(delete(OBJETIVOS + "/{id}/mini-objetivos/{mini}", ajeno, miniAjeno)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Los cambios exigen el token CSRF")
    void cambiosShouldRequireCsrf() throws Exception {
        mockMvc.perform(delete(OBJETIVOS + "/{id}/mini-objetivos/{mini}", UUID.randomUUID(), UUID.randomUUID())
                        .session(sesion))
                .andExpect(status().isForbidden());
    }

    // --- act ---
    private String whenCrearObjetivo() throws Exception {
        return whenCrearObjetivo(500000);
    }

    private String whenCrearObjetivo(int meta) throws Exception {
        String body = mockMvc.perform(post(OBJETIVOS).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType("application/json")
                        .content("{\"texto\":\"Quiero ganar más\",\"meta\":" + meta + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String whenListar(String objetivoId) throws Exception {
        return mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andReturn().getResponse().getContentAsString();
    }

    // --- arrange ---
    private UUID givenUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Mini-objetivos HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Mini-objetivos HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }

    private UUID givenObjetivoDeOtroNegocio() {
        UUID otroUsuario = givenUsuarioYNegocio();
        UUID otroNegocio = jdbcTemplate.queryForObject(
                "SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, otroUsuario);
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
