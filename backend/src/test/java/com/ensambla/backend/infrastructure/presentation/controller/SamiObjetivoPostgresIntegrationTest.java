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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Armar un objetivo hablando con Sami de punta a punta con PostgreSQL y el proveedor de IA simulado: la propuesta con
 * los datos reales del negocio, guardar lo que se aceptó y que el objetivo sin meta conviva con el resto.
 */
@Transactional
class SamiObjetivoPostgresIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String PROPUESTA = "/api/objetivos/sami/propuesta";
    private static final String OBJETIVOS = "/api/objetivos";
    private static final String OBJETIVO = "Quiero vender más y que me conozcan en el barrio";
    private static final String CONVERSACION =
            "{\"mensajes\":[{\"rol\":\"PERSONA\",\"texto\":\"" + OBJETIVO + "\"}]}";
    private static final JsonMapper JSON = JsonMapper.builder().build();

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
    @DisplayName("Sami propone una métrica del catálogo con números reales y una acción, y dice en qué datos se apoya")
    void proponerShouldProposeAMeasuredMetricAndAManualAction() throws Exception {
        whenPropuesta(CONVERSACION)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("IA"))
                .andExpect(jsonPath("$.tipo").value("HIBRIDO"))
                .andExpect(jsonPath("$.esMonetario").value(false))
                .andExpect(jsonPath("$.puedeGuardar").value(true))
                .andExpect(jsonPath("$.mensajesRestantes").value(5))
                .andExpect(jsonPath("$.minis.length()").value(2))
                .andExpect(jsonPath("$.minis[0].modo").value("MEDIDO"))
                .andExpect(jsonPath("$.minis[0].meta").isNumber())
                .andExpect(jsonPath("$.minis[0].aporteEstimado").isNumber())
                .andExpect(jsonPath("$.minis[1].modo").value("MANUAL"))
                .andExpect(jsonPath("$.minis[1].meta").doesNotExist())
                .andExpect(jsonPath("$.minis[1].tipo").doesNotExist())
                .andExpect(jsonPath("$.datos.length()").value(5))
                .andExpect(jsonPath("$.datos[0].fuente").value("VENTAS"))
                .andExpect(jsonPath("$.datos[0].hay").value(true));
    }

    @Test
    @DisplayName("Proponer no guarda nada ni gasta cuota")
    void proponerShouldNotSaveAnything() throws Exception {
        whenPropuesta(CONVERSACION).andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM objetivo WHERE negocio_id = ?",
                Integer.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Lo que la persona acepta se guarda como objetivo híbrido sin meta, con sus mini-objetivos")
    void guardarShouldSaveTheAcceptedProposal() throws Exception {
        String propuesta = whenPropuesta(CONVERSACION).andReturn().getResponse().getContentAsString();

        String objetivo = whenGuardar(aceptando(propuesta, false))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("HIBRIDO"))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.armadoConSami").value(true))
                .andExpect(jsonPath("$.texto").value(OBJETIVO))
                .andReturn().getResponse().getContentAsString();

        String objetivoId = JsonPath.read(objetivo, "$.id");
        mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(2))
                .andExpect(jsonPath("$.minis[0].modo").value("MEDIDO"))
                .andExpect(jsonPath("$.minis[0].posicion").value(1))
                .andExpect(jsonPath("$.minis[1].modo").value("MANUAL"))
                .andExpect(jsonPath("$.minis[1].posicion").value(2))
                .andExpect(jsonPath("$.minis[1].meta").doesNotExist());
        mockMvc.perform(get(OBJETIVOS).session(sesion))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("HIBRIDO"))
                .andExpect(jsonPath("$[0].meta").doesNotExist());
    }

    @Test
    @DisplayName("Los números del mini-objetivo medido los pone el servidor aunque el frontend no los mande")
    void guardarShouldRecomputeTheNumbersOfAMeasuredMini() throws Exception {
        String propuesta = whenPropuesta(CONVERSACION).andReturn().getResponse().getContentAsString();
        Number metaPropuesta = JsonPath.read(propuesta, "$.minis[0].meta");

        String objetivoId = JsonPath.read(whenGuardar(aceptando(propuesta, false))
                .andReturn().getResponse().getContentAsString(), "$.id");

        Number metaGuardada = jdbcTemplate.queryForObject(
                "SELECT meta FROM mini_objetivo WHERE objetivo_id = ? AND modo = 'MEDIDO'", Number.class,
                UUID.fromString(objetivoId));
        assertThat(metaGuardada.doubleValue()).isEqualTo(metaPropuesta.doubleValue());
    }

    @Test
    @DisplayName("Un objetivo sin meta convive con el resto: progreso, pendientes y respuesta del manual")
    void guardarShouldLetTheObjetivoWithoutGoalWorkWithTheRest() throws Exception {
        String propuesta = whenPropuesta(CONVERSACION).andReturn().getResponse().getContentAsString();
        String objetivoId = JsonPath.read(whenGuardar(aceptando(propuesta, false))
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", objetivoId).session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.minis.length()").value(1));
        String pendientes = mockMvc.perform(get(OBJETIVOS + "/respuestas-pendientes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objetivoId").value(objetivoId))
                .andReturn().getResponse().getContentAsString();
        String manualId = JsonPath.read(pendientes, "$[0].miniObjetivo.id");
        mockMvc.perform(post(OBJETIVOS + "/{o}/mini-objetivos/{m}/respuesta", objetivoId, manualId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                        .content("{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(post(OBJETIVOS + "/medir").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Guardar como seguimiento manual crea un objetivo sin IA con un solo mini-objetivo manual")
    void guardarShouldSaveTheManualFollowUp() throws Exception {
        whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"seguimientoManual\":true}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SIN_DATOS"))
                .andExpect(jsonPath("$.armadoConSami").value(false));

        assertThat(jdbcTemplate.queryForList(
                "SELECT m.modo FROM mini_objetivo m JOIN objetivo o ON o.id = m.objetivo_id WHERE o.negocio_id = ?",
                String.class, negocioId)).containsExactly("MANUAL");
    }

    @Test
    @DisplayName("Guardar un mini-objetivo medido que no está en el catálogo responde 400 y no guarda nada")
    void guardarShouldRejectAMetricThatIsNotInTheCatalog() throws Exception {
        whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"minis\":[{\"modo\":\"MEDIDO\",\"tipo\":\"RECORTAR_GASTO\","
                + "\"referencia\":\"Una categoría que no existe\"}]}")
                .andExpect(status().isBadRequest());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM objetivo WHERE negocio_id = ?",
                Integer.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Guardar una acción con un importe responde 400")
    void guardarShouldRejectAnActionWithAnAmount() throws Exception {
        whenGuardar("{\"texto\":\"" + OBJETIVO + "\",\"minis\":[{\"modo\":\"MANUAL\","
                + "\"titulo\":\"Cobrá $5.000 más\",\"justificacion\":\"Porque sí\"}]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Con más mensajes que el tope la conversación responde 400")
    void proponerShouldRejectAConversationOverTheLimit() throws Exception {
        String mensajes = String.join(",", Collections.nCopies(7,
                "{\"rol\":\"PERSONA\",\"texto\":\"Quiero vender más\"}"));

        whenPropuesta("{\"mensajes\":[" + mensajes + "]}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Proponer y guardar piden sesión y las escrituras piden CSRF")
    void shouldRequireSessionAndCsrf() throws Exception {
        mockMvc.perform(post(PROPUESTA).contentType("application/json").content(CONVERSACION))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(OBJETIVOS + "/sami").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post(PROPUESTA).session(sesion).contentType("application/json").content(CONVERSACION))
                .andExpect(status().isForbidden());
    }

    // --- act ---
    private ResultActions whenPropuesta(String cuerpo) throws Exception {
        return mockMvc.perform(post(PROPUESTA).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(cuerpo));
    }

    private ResultActions whenGuardar(String cuerpo) throws Exception {
        return mockMvc.perform(post(OBJETIVOS + "/sami").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(cuerpo));
    }

    // --- helpers ---
    /** El pedido de guardar que arma el frontend: de cada mini-objetivo propuesto sólo se manda cuál es. */
    private static String aceptando(String propuesta, boolean seguimientoManual) throws Exception {
        List<Map<String, Object>> minis = new ArrayList<>();
        int cantidad = JsonPath.<List<Object>>read(propuesta, "$.minis").size();
        for (int indice = 0; indice < cantidad; indice++) {
            String base = "$.minis[" + indice + "].";
            Map<String, Object> mini = new LinkedHashMap<>();
            mini.put("modo", JsonPath.read(propuesta, base + "modo"));
            mini.put("tipo", JsonPath.read(propuesta, base + "tipo"));
            mini.put("registroId", JsonPath.read(propuesta, base + "registroId"));
            mini.put("referencia", JsonPath.read(propuesta, base + "referencia"));
            mini.put("titulo", JsonPath.read(propuesta, base + "titulo"));
            mini.put("justificacion", JsonPath.read(propuesta, base + "justificacion"));
            minis.add(mini);
        }
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("texto", OBJETIVO);
        cuerpo.put("seguimientoManual", seguimientoManual);
        cuerpo.put("minis", minis);
        return JSON.writeValueAsString(cuerpo);
    }

    private UUID givenUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Objetivos con Sami HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Objetivos con Sami HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
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
