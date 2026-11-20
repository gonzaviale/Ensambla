package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
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
    private static final String MINI_OBJETIVOS = OBJETIVOS + "/{id}/mini-objetivos";

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
    private SesionHttpDePrueba sesion;
    private UUID negocioId;
    private String objetivoId;
    private String miniId;
    private String cuerpoDelReemplazo;
    private UUID objetivoAjenoId;
    private ResultActions respuesta;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = crearUsuarioYNegocio();
        negocioId = jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        sesion = SesionHttpDePrueba.autenticada(mockMvc, usuarioId.toString());
    }

    @Test
    @DisplayName("Un objetivo nuevo trae 3 mini-objetivos distintos, con meta y valor actual calculados con sus datos")
    void unObjetivoNuevoTrae3MiniObjetivosDistintosConMetaYValorActualCalculadosConSusDatos() throws Exception {
        dadoUnObjetivoNuevo(500000);

        cuandoSeListanLosMiniObjetivos();

        entoncesSeProponenTresMiniObjetivosDistintos();
    }

    @Test
    @DisplayName("Un objetivo cuya meta ya se alcanza nace alcanzado y sin mini-objetivos")
    void unObjetivoCuyaMetaYaSeAlcanzaNaceAlcanzadoYSinMiniObjetivos() throws Exception {
        dadoUnObjetivoNuevo(1000);

        cuandoSeListanLosMiniObjetivos();

        entoncesElObjetivoNaceAlcanzadoSinMiniObjetivos();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde 404")
    void unObjetivoDeOtroNegocioResponde404() throws Exception {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeListanLosMiniObjetivosDe(objetivoAjenoId.toString());

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Un objetivo inexistente responde 404 igual que uno de otro negocio")
    void unObjetivoInexistenteResponde404IgualQueUnoDeOtroNegocio() throws Exception {
        cuandoSeListanLosMiniObjetivosDe(UUID.randomUUID().toString());

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Sin sesión responde 401")
    void sinSesionResponde401() throws Exception {
        cuandoSeListanLosMiniObjetivosSinSesion();

        entoncesNoAutoriza();
    }

    @Test
    @DisplayName("Cambiar un mini-objetivo por una alternativa lo deja en su lugar y el viejo queda en el historial")
    void cambiarUnMiniObjetivoPorUnaAlternativaLoDejaEnSuLugarYElViejoQuedaEnElHistorial() throws Exception {
        dadoUnObjetivoNuevoConSuPrimerMiniObjetivo();
        dadoLaPrimeraAlternativaComoElegida();

        cuandoSeReemplazaElMiniObjetivo();

        entoncesElNuevoQuedaEnSuLugarYElViejoEnElHistorial();
    }

    @Test
    @DisplayName("Elegir algo que no es una alternativa responde 400 y no cambia nada")
    void elegirAlgoQueNoEsUnaAlternativaResponde400YNoCambiaNada() throws Exception {
        dadoUnObjetivoNuevoConSuPrimerMiniObjetivo();
        dadoUnaEleccionQueNoEsUnaAlternativa();

        cuandoSeReemplazaElMiniObjetivo();

        entoncesSeRechazaLaEleccionYNoCambiaNada();
    }

    @Test
    @DisplayName("Quitar un mini-objetivo responde 204, lo deja en el historial y libera su lugar")
    void quitarUnMiniObjetivoResponde204LoDejaEnElHistorialYLiberaSuLugar() throws Exception {
        dadoUnObjetivoNuevoConSuTercerMiniObjetivo();

        cuandoSeQuitaElMiniObjetivo();

        entoncesElMiniObjetivoQuedaEnElHistorialYLiberaSuLugar();
    }

    @Test
    @DisplayName("Quitar un mini-objetivo de otro negocio responde 404 y lo conserva")
    void quitarUnMiniObjetivoDeOtroNegocioResponde404YLoConserva() throws Exception {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeQuitaUnMiniObjetivoDeOtroNegocio();

        entoncesRespondeNoEncontrado();
    }

    @Test
    @DisplayName("Los cambios exigen el token CSRF")
    void losCambiosExigenElTokenCsrf() throws Exception {
        cuandoSeQuitaUnMiniObjetivoSinCsrf();

        entoncesSeProhibe();
    }

    // --- dado ---

    private void dadoUnObjetivoNuevo(int meta) throws Exception {
        String cuerpo = mockMvc.perform(post(OBJETIVOS).session(sesion.sesion())
                        .header("X-CSRF-TOKEN", sesion.tokenCsrf())
                        .contentType("application/json")
                        .content("{\"texto\":\"Quiero ganar más\",\"meta\":" + meta + "}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        objetivoId = JsonPath.read(cuerpo, "$.id");
    }

    private void dadoUnObjetivoNuevoConSuPrimerMiniObjetivo() throws Exception {
        dadoUnObjetivoNuevo(500000);
        miniId = JsonPath.read(contenidoDeLosMiniObjetivos(), "$.minis[0].id");
    }

    private void dadoUnObjetivoNuevoConSuTercerMiniObjetivo() throws Exception {
        dadoUnObjetivoNuevo(500000);
        miniId = JsonPath.read(contenidoDeLosMiniObjetivos(), "$.minis[2].id");
    }

    private void dadoLaPrimeraAlternativaComoElegida() throws Exception {
        String candidatos = mockMvc.perform(get(MINI_OBJETIVOS + "/{mini}/candidatos", objetivoId, miniId)
                        .session(sesion.sesion()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> elegido = new LinkedHashMap<>();
        elegido.put("tipo", JsonPath.read(candidatos, "$[0].tipo"));
        elegido.put("registroId", JsonPath.read(candidatos, "$[0].registroId"));
        elegido.put("referencia", JsonPath.read(candidatos, "$[0].referencia"));
        cuerpoDelReemplazo = JsonMapper.builder().build().writeValueAsString(elegido);
    }

    private void dadoUnaEleccionQueNoEsUnaAlternativa() {
        cuerpoDelReemplazo = "{\"tipo\":\"RECORTAR_GASTO\",\"referencia\":\"Una categoría que no existe\"}";
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        UUID otroUsuario = crearUsuarioYNegocio();
        UUID otroNegocio = jdbcTemplate.queryForObject(
                "SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, otroUsuario);
        objetivoAjenoId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO objetivo (id, negocio_id, texto, meta) VALUES (?, ?, 'Ajeno', 400000)",
                objetivoAjenoId, otroNegocio);
    }

    // --- cuando ---

    private void cuandoSeListanLosMiniObjetivos() throws Exception {
        cuandoSeListanLosMiniObjetivosDe(objetivoId);
    }

    private void cuandoSeListanLosMiniObjetivosDe(String objetivo) throws Exception {
        respuesta = mockMvc.perform(get(MINI_OBJETIVOS, objetivo).session(sesion.sesion()));
    }

    private void cuandoSeListanLosMiniObjetivosSinSesion() throws Exception {
        respuesta = mockMvc.perform(get(MINI_OBJETIVOS, UUID.randomUUID()));
    }

    private void cuandoSeReemplazaElMiniObjetivo() throws Exception {
        respuesta = mockMvc.perform(post(MINI_OBJETIVOS + "/{mini}/reemplazo", objetivoId, miniId)
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf())
                .contentType("application/json").content(cuerpoDelReemplazo));
    }

    private void cuandoSeQuitaElMiniObjetivo() throws Exception {
        respuesta = mockMvc.perform(delete(MINI_OBJETIVOS + "/{mini}", objetivoId, miniId)
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    private void cuandoSeQuitaUnMiniObjetivoDeOtroNegocio() throws Exception {
        respuesta = mockMvc.perform(delete(MINI_OBJETIVOS + "/{mini}", objetivoAjenoId, UUID.randomUUID())
                .session(sesion.sesion()).header("X-CSRF-TOKEN", sesion.tokenCsrf()));
    }

    private void cuandoSeQuitaUnMiniObjetivoSinCsrf() throws Exception {
        respuesta = mockMvc.perform(delete(MINI_OBJETIVOS + "/{mini}", UUID.randomUUID(), UUID.randomUUID())
                .session(sesion.sesion()));
    }

    // --- entonces ---

    private void entoncesSeProponenTresMiniObjetivosDistintos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.minis[0].posicion").value(1))
                .andExpect(jsonPath("$.minis[1].posicion").value(2))
                .andExpect(jsonPath("$.minis[2].posicion").value(3))
                .andExpect(jsonPath("$.minis[0].estado").value("EN_CURSO"))
                .andExpect(jsonPath("$.minis[0].textoConIa").value(false))
                .andExpect(jsonPath("$.historial.length()").value(0))
                .andExpect(jsonPath("$.faltanDatos").doesNotExist());
        List<String> tipos = JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(),
                "$.minis[*].tipo");
        assertThat(tipos).doesNotHaveDuplicates();
    }

    private void entoncesElObjetivoNaceAlcanzadoSinMiniObjetivos() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(0))
                .andExpect(jsonPath("$.faltanDatos").value("OBJETIVO_ALCANZADO"));
    }

    private void entoncesRespondeNoEncontrado() throws Exception {
        respuesta.andExpect(status().isNotFound());
    }

    private void entoncesNoAutoriza() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesElNuevoQuedaEnSuLugarYElViejoEnElHistorial() throws Exception {
        respuesta.andExpect(status().isOk())
                .andExpect(jsonPath("$.posicion").value(1))
                .andExpect(jsonPath("$.estado").value("EN_CURSO"));
        mockMvc.perform(get(MINI_OBJETIVOS, objetivoId).session(sesion.sesion()))
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.historial.length()").value(1))
                .andExpect(jsonPath("$.historial[0].id").value(miniId))
                .andExpect(jsonPath("$.historial[0].estado").value("REEMPLAZADO"));
    }

    private void entoncesSeRechazaLaEleccionYNoCambiaNada() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        mockMvc.perform(get(MINI_OBJETIVOS, objetivoId).session(sesion.sesion()))
                .andExpect(jsonPath("$.minis.length()").value(3))
                .andExpect(jsonPath("$.historial.length()").value(0));
    }

    private void entoncesElMiniObjetivoQuedaEnElHistorialYLiberaSuLugar() throws Exception {
        respuesta.andExpect(status().isNoContent());
        mockMvc.perform(get(MINI_OBJETIVOS, objetivoId).session(sesion.sesion()))
                .andExpect(jsonPath("$.minis.length()").value(2))
                .andExpect(jsonPath("$.historial[0].estado").value("ABANDONADO"))
                .andExpect(jsonPath("$.faltanDatos").value("POCOS_DATOS"));
    }

    private void entoncesSeProhibe() throws Exception {
        respuesta.andExpect(status().isForbidden());
    }

    // --- datos ---

    private String contenidoDeLosMiniObjetivos() throws Exception {
        return mockMvc.perform(get(MINI_OBJETIVOS, objetivoId).session(sesion.sesion()))
                .andReturn().getResponse().getContentAsString();
    }

    private UUID crearUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Mini-objetivos HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Mini-objetivos HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
    }
}
