package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.infrastructure.presentation.dto.auth.CsrfResponse;
import com.ensambla.backend.support.AlmacenDeObjetivosDePrueba;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    private static final String SEGUIMIENTO_MANUAL = "{\"texto\":\"" + OBJETIVO + "\",\"seguimientoManual\":true}";
    private static final int CUOTA_MENSUAL_GRATUITA = 5;
    private static final int MENSAJES_DE_MAS = 7;
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

    @Autowired
    private SuscripcionesDePrueba suscripciones;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID negocioId;
    private String propuesta;
    private String objetivoId;
    private ResultActions respuestaPropuesta;
    private ResultActions respuestaGuardar;
    private ResultActions respuestaPlanes;
    private ResultActions respuestaEliminar;
    private ResultActions respuestaMinis;
    private ResultActions respuestaLista;
    private ResultActions respuestaProgreso;
    private ResultActions respuestaPendientes;
    private ResultActions respuestaDelManual;
    private ResultActions respuestaMedir;
    private String pendientes;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        UUID usuarioId = dadoUsuarioYNegocio();
        negocioId = jdbcTemplate.queryForObject("SELECT id FROM negocio WHERE duenio_id = ?", UUID.class, usuarioId);
        AlmacenDeObjetivosDePrueba.guardar(sistemaRepository, negocioId);
        suscripciones.asignarGratuito(NegocioId.de(negocioId), OffsetDateTime.now().minusDays(1));
        sesion = dadoSesion(usuarioId);
    }

    @Test
    @DisplayName("Sami propone una métrica del catálogo con números reales y una acción, y dice en qué datos se apoya")
    void samiProponeUnaMetricaDelCatalogoConNumerosRealesYUnaAccionYDiceEnQueDatosSe() throws Exception {
        cuandoSePideUnaPropuesta();

        entoncesSamiProponeUnaMetricaYUnaAccionConSusDatos();
    }

    @Test
    @DisplayName("La propuesta dice cuántos objetivos con IA lleva el mes y cuántos permite el plan")
    void laPropuestaDiceCuantosObjetivosConIaLlevaElMesYCuantosPermiteElPlan() throws Exception {
        cuandoSePideUnaPropuesta();

        entoncesLaCuotaEs(0, CUOTA_MENSUAL_GRATUITA, CUOTA_MENSUAL_GRATUITA);
    }

    @Test
    @DisplayName("Guardar con IA gasta una unidad de la cuota, que se ve en la propuesta y en Planes")
    void guardarConIaGastaUnaUnidadDeLaCuotaQueSeVeEnLaPropuestaYEnPlanes() throws Exception {
        dadoUnObjetivoGuardadoConLoQueSamiPropuso();

        cuandoSePideUnaPropuesta();
        cuandoSePidenLosPlanes();

        entoncesLaCuotaEs(1, CUOTA_MENSUAL_GRATUITA, 4);
        entoncesPlanesMuestraElUsoYLosLimites();
    }

    @Test
    @DisplayName("Con la cuota agotada la propuesta no usa la IA y guardar con IA responde 403 LIMITE_OBJETIVOS_IA")
    void shouldStopWhenTheAllowanceIsUsed() throws Exception {
        dadoUnaPropuestaDeSami();
        dadoCuotaAgotada();

        cuandoSePideUnaPropuesta();
        cuandoSeGuardaLoAceptado();

        entoncesLaPropuestaEsDeRespaldoYSinCuota();
        entoncesGuardarResponde403PorLimite();
        entoncesNoSeGuardoNingunObjetivo();
    }

    @Test
    @DisplayName("Con la cuota agotada el seguimiento manual se guarda igual y no gasta nada")
    void conLaCuotaAgotadaElSeguimientoManualSeGuardaIgualYNoGastaNada() throws Exception {
        dadoCuotaAgotada();

        cuandoSeGuarda(SEGUIMIENTO_MANUAL);

        entoncesSeCreo();
        entoncesElConsumoSigueEnLaCuotaMensual();
    }

    @Test
    @DisplayName("Eliminar el objetivo no devuelve la cuota del mes")
    void eliminarElObjetivoNoDevuelveLaCuotaDelMes() throws Exception {
        dadoUnObjetivoGuardadoConLoQueSamiPropuso();

        cuandoSeEliminaElObjetivo();
        cuandoSePideUnaPropuesta();

        entoncesSeElimino();
        entoncesLaCuotaUsadaEs(1);
    }

    @Test
    @DisplayName("Proponer no guarda nada ni gasta cuota")
    void proponerNoGuardaNadaNiGastaCuota() throws Exception {
        cuandoSePideUnaPropuesta();

        entoncesSeResponde200();
        entoncesNoSeGuardoNingunObjetivo();
    }

    @Test
    @DisplayName("Lo que la persona acepta se guarda como objetivo híbrido sin meta, con sus mini-objetivos")
    void loQueLaPersonaAceptaSeGuardaComoObjetivoHibridoSinMetaConSusMiniObjetivos() throws Exception {
        dadoUnaPropuestaDeSami();

        cuandoSeGuardaLoAceptado();
        cuandoSePidenLosMinisYLaListaDelObjetivoGuardado();

        entoncesSeGuardaUnObjetivoHibridoSinMeta();
        entoncesSeGuardanSusDosMinisEnOrden();
        entoncesLaListaTieneElObjetivoHibridoSinMeta();
    }

    @Test
    @DisplayName("Los números del mini-objetivo medido los pone el servidor aunque el frontend no los mande")
    void losNumerosDelMiniObjetivoMedidoLosPoneElServidorAunqueElFrontendNoLosMande() throws Exception {
        dadoUnObjetivoGuardadoConLoQueSamiPropuso();

        entoncesLaMetaGuardadaDelMedidoEsLaDeLaPropuesta();
    }

    @Test
    @DisplayName("Un objetivo sin meta convive con el resto: progreso, pendientes y respuesta del manual")
    void unObjetivoSinMetaConviveConElRestoProgresoPendientesYRespuestaDelManual() throws Exception {
        dadoUnObjetivoGuardadoConLoQueSamiPropuso();

        cuandoSePideElProgreso();
        cuandoSePidenLosPendientes();
        cuandoSeRespondeElManualPendiente();
        cuandoSeMidenLosObjetivos();

        entoncesElProgresoNoTieneMetaYTieneUnMini();
        entoncesElUnicoPendienteEsElDelObjetivo();
        entoncesSeCreoLaRespuestaDelManual();
        entoncesSeMidioSinContenido();
    }

    @Test
    @DisplayName("Guardar como seguimiento manual crea un objetivo sin IA con un solo mini-objetivo manual")
    void guardarComoSeguimientoManualCreaUnObjetivoSinIaConUnSoloMiniObjetivoManual() throws Exception {
        cuandoSeGuarda(SEGUIMIENTO_MANUAL);

        entoncesSeCreaUnObjetivoSinDatosYSinIa();
        entoncesTieneUnSoloMiniManual();
    }

    @Test
    @DisplayName("Guardar un mini-objetivo medido que no está en el catálogo responde 400 y no guarda nada")
    void guardarUnMiniObjetivoMedidoQueNoEstaEnElCatalogoResponde400YNoGuardaNada() throws Exception {
        cuandoSeGuarda("{\"texto\":\"" + OBJETIVO + "\",\"minis\":[{\"modo\":\"MEDIDO\",\"tipo\":\"RECORTAR_GASTO\","
                + "\"referencia\":\"Una categoría que no existe\"}]}");

        entoncesGuardarResponde400();
        entoncesNoSeGuardoNingunObjetivo();
    }

    @Test
    @DisplayName("Guardar una acción con un importe responde 400")
    void guardarUnaAccionConUnImporteResponde400() throws Exception {
        cuandoSeGuarda("{\"texto\":\"" + OBJETIVO + "\",\"minis\":[{\"modo\":\"MANUAL\","
                + "\"titulo\":\"Cobrá $5.000 más\",\"justificacion\":\"Porque sí\"}]}");

        entoncesGuardarResponde400();
    }

    @Test
    @DisplayName("Con más mensajes que el tope la conversación responde 400")
    void conMasMensajesQueElTopeLaConversacionResponde400() throws Exception {
        cuandoSePideUnaPropuestaCon(conversacionConMensajes(MENSAJES_DE_MAS));

        entoncesProponerResponde400();
    }

    @Test
    @DisplayName("Proponer y guardar piden sesión y las escrituras piden CSRF")
    void shouldRequireSessionAndCsrf() throws Exception {
        entoncesProponerSinSesionResponde401();
        entoncesGuardarSinSesionResponde401();
        entoncesProponerSinCsrfResponde403();
    }

    // --- arrange ---
    private void dadoUnaPropuestaDeSami() throws Exception {
        cuandoSePideUnaPropuesta();
        propuesta = respuestaPropuesta.andReturn().getResponse().getContentAsString();
    }

    private void dadoUnObjetivoGuardadoConLoQueSamiPropuso() throws Exception {
        dadoUnaPropuestaDeSami();
        cuandoSeGuardaLoAceptado();
        objetivoId = JsonPath.read(respuestaGuardar.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void dadoCuotaAgotada() {
        IntStream.range(0, CUOTA_MENSUAL_GRATUITA).forEach(indice -> jdbcTemplate.update(
                "INSERT INTO consumo_objetivo_ia (negocio_id, objetivo_id, consumido_en) VALUES (?, ?, now())",
                negocioId, UUID.randomUUID()));
    }

    // --- act ---
    private void cuandoSePideUnaPropuesta() throws Exception {
        cuandoSePideUnaPropuestaCon(CONVERSACION);
    }

    private void cuandoSePideUnaPropuestaCon(String cuerpo) throws Exception {
        respuestaPropuesta = mockMvc.perform(post(PROPUESTA).session(sesion).header("X-CSRF-TOKEN", csrfToken)
                .contentType("application/json").content(cuerpo));
    }

    private void cuandoSeGuardaLoAceptado() throws Exception {
        cuandoSeGuarda(aceptando(propuesta, false));
    }

    private void cuandoSeGuarda(String cuerpo) throws Exception {
        respuestaGuardar = mockMvc.perform(post(OBJETIVOS + "/sami").session(sesion)
                .header("X-CSRF-TOKEN", csrfToken).contentType("application/json").content(cuerpo));
    }

    private void cuandoSePidenLosPlanes() throws Exception {
        respuestaPlanes = mockMvc.perform(get("/api/planes").session(sesion));
    }

    private void cuandoSeEliminaElObjetivo() throws Exception {
        respuestaEliminar = mockMvc.perform(delete(OBJETIVOS + "/{id}", objetivoId).session(sesion)
                .header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSePidenLosMinisYLaListaDelObjetivoGuardado() throws Exception {
        objetivoId = JsonPath.read(respuestaGuardar.andReturn().getResponse().getContentAsString(), "$.id");
        respuestaMinis = mockMvc.perform(get(OBJETIVOS + "/{id}/mini-objetivos", objetivoId).session(sesion));
        respuestaLista = mockMvc.perform(get(OBJETIVOS).session(sesion));
    }

    private void cuandoSePideElProgreso() throws Exception {
        respuestaProgreso = mockMvc.perform(get(OBJETIVOS + "/{id}/progreso", objetivoId).session(sesion));
    }

    private void cuandoSePidenLosPendientes() throws Exception {
        respuestaPendientes = mockMvc.perform(get(OBJETIVOS + "/respuestas-pendientes").session(sesion));
        pendientes = respuestaPendientes.andReturn().getResponse().getContentAsString();
    }

    private void cuandoSeRespondeElManualPendiente() throws Exception {
        String manualId = JsonPath.read(pendientes, "$[0].miniObjetivo.id");
        respuestaDelManual = mockMvc.perform(
                post(OBJETIVOS + "/{o}/mini-objetivos/{m}/respuesta", objetivoId, manualId)
                        .session(sesion).header("X-CSRF-TOKEN", csrfToken).contentType("application/json")
                        .content("{\"hizo\":\"SI\",\"quePaso\":\"FUNCIONO\"}"));
    }

    private void cuandoSeMidenLosObjetivos() throws Exception {
        respuestaMedir = mockMvc.perform(post(OBJETIVOS + "/medir").session(sesion)
                .header("X-CSRF-TOKEN", csrfToken));
    }

    // --- assert ---
    private void entoncesSamiProponeUnaMetricaYUnaAccionConSusDatos() throws Exception {
        respuestaPropuesta
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

    private void entoncesLaCuotaEs(int usadas, int limite, int restantes) throws Exception {
        respuestaPropuesta
                .andExpect(jsonPath("$.cuota.usadas").value(usadas))
                .andExpect(jsonPath("$.cuota.limite").value(limite))
                .andExpect(jsonPath("$.cuota.restantes").value(restantes));
    }

    private void entoncesLaCuotaUsadaEs(int usadas) throws Exception {
        respuestaPropuesta.andExpect(jsonPath("$.cuota.usadas").value(usadas));
    }

    private void entoncesPlanesMuestraElUsoYLosLimites() throws Exception {
        respuestaPlanes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uso.objetivosIa").value(1))
                .andExpect(jsonPath("$.uso.limiteObjetivosIa").value(5))
                .andExpect(jsonPath("$.planes[0].limiteObjetivosIaMensuales").value(5))
                .andExpect(jsonPath("$.planes[1].limiteObjetivosIaMensuales").value(20));
    }

    private void entoncesLaPropuestaEsDeRespaldoYSinCuota() throws Exception {
        respuestaPropuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origen").value("RESPALDO"))
                .andExpect(jsonPath("$.respuesta").value(startsWith("Ya armaste los 5 objetivos")))
                .andExpect(jsonPath("$.cuota.restantes").value(0));
    }

    private void entoncesGuardarResponde403PorLimite() throws Exception {
        respuestaGuardar
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("LIMITE_OBJETIVOS_IA"));
    }

    private void entoncesNoSeGuardoNingunObjetivo() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM objetivo WHERE negocio_id = ?",
                Integer.class, negocioId)).isZero();
    }

    private void entoncesSeCreo() throws Exception {
        respuestaGuardar.andExpect(status().isCreated());
    }

    private void entoncesElConsumoSigueEnLaCuotaMensual() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM consumo_objetivo_ia WHERE negocio_id = ?",
                Integer.class, negocioId)).isEqualTo(CUOTA_MENSUAL_GRATUITA);
    }

    private void entoncesSeElimino() throws Exception {
        respuestaEliminar.andExpect(status().isNoContent());
    }

    private void entoncesSeResponde200() throws Exception {
        respuestaPropuesta.andExpect(status().isOk());
    }

    private void entoncesSeGuardaUnObjetivoHibridoSinMeta() throws Exception {
        respuestaGuardar
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("HIBRIDO"))
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.armadoConSami").value(true))
                .andExpect(jsonPath("$.texto").value(OBJETIVO));
    }

    private void entoncesSeGuardanSusDosMinisEnOrden() throws Exception {
        respuestaMinis
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minis.length()").value(2))
                .andExpect(jsonPath("$.minis[0].modo").value("MEDIDO"))
                .andExpect(jsonPath("$.minis[0].posicion").value(1))
                .andExpect(jsonPath("$.minis[1].modo").value("MANUAL"))
                .andExpect(jsonPath("$.minis[1].posicion").value(2))
                .andExpect(jsonPath("$.minis[1].meta").doesNotExist());
    }

    private void entoncesLaListaTieneElObjetivoHibridoSinMeta() throws Exception {
        respuestaLista
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tipo").value("HIBRIDO"))
                .andExpect(jsonPath("$[0].meta").doesNotExist());
    }

    private void entoncesLaMetaGuardadaDelMedidoEsLaDeLaPropuesta() {
        Number metaPropuesta = JsonPath.read(propuesta, "$.minis[0].meta");
        Number metaGuardada = jdbcTemplate.queryForObject(
                "SELECT meta FROM mini_objetivo WHERE objetivo_id = ? AND modo = 'MEDIDO'", Number.class,
                UUID.fromString(objetivoId));
        assertThat(metaGuardada.doubleValue()).isEqualTo(metaPropuesta.doubleValue());
    }

    private void entoncesElProgresoNoTieneMetaYTieneUnMini() throws Exception {
        respuestaProgreso
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta").doesNotExist())
                .andExpect(jsonPath("$.minis.length()").value(1));
    }

    private void entoncesElUnicoPendienteEsElDelObjetivo() throws Exception {
        respuestaPendientes
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].objetivoId").value(objetivoId));
    }

    private void entoncesSeCreoLaRespuestaDelManual() throws Exception {
        respuestaDelManual.andExpect(status().isCreated());
    }

    private void entoncesSeMidioSinContenido() throws Exception {
        respuestaMedir.andExpect(status().isNoContent());
    }

    private void entoncesSeCreaUnObjetivoSinDatosYSinIa() throws Exception {
        respuestaGuardar
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("SIN_DATOS"))
                .andExpect(jsonPath("$.armadoConSami").value(false));
    }

    private void entoncesTieneUnSoloMiniManual() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT m.modo FROM mini_objetivo m JOIN objetivo o ON o.id = m.objetivo_id WHERE o.negocio_id = ?",
                String.class, negocioId)).containsExactly("MANUAL");
    }

    private void entoncesGuardarResponde400() throws Exception {
        respuestaGuardar.andExpect(status().isBadRequest());
    }

    private void entoncesProponerResponde400() throws Exception {
        respuestaPropuesta.andExpect(status().isBadRequest());
    }

    private void entoncesProponerSinSesionResponde401() throws Exception {
        mockMvc.perform(post(PROPUESTA).contentType("application/json").content(CONVERSACION))
                .andExpect(status().isUnauthorized());
    }

    private void entoncesGuardarSinSesionResponde401() throws Exception {
        mockMvc.perform(post(OBJETIVOS + "/sami").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private void entoncesProponerSinCsrfResponde403() throws Exception {
        mockMvc.perform(post(PROPUESTA).session(sesion).contentType("application/json").content(CONVERSACION))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---
    private static String conversacionConMensajes(int cantidad) {
        String mensajes = String.join(",", Collections.nCopies(cantidad,
                "{\"rol\":\"PERSONA\",\"texto\":\"Quiero vender más\"}"));
        return "{\"mensajes\":[" + mensajes + "]}";
    }

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

    private UUID dadoUsuarioYNegocio() {
        UUID usuarioId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Objetivos con Sami HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Objetivos con Sami HTTP', ?)",
                UUID.randomUUID(), usuarioId);
        return usuarioId;
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
