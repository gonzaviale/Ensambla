package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class PlanSeguridadIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String PEDIDO = """
            {"nombre":"Ventas","destinoExistente":true,"contexto":"registros","tipo":"sales",
             "campos":[{"key":"nombre","label":"Nombre","type":"text","required":false}]}
            """;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SuscripcionService suscripcionService;

    @Autowired
    private Clock clock;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UUID usuarioId;
    private UUID negocioId;
    private UUID proyectoId;
    private UUID bloqueId;
    private UUID registroId;
    private OffsetDateTime ahora;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        ahora = OffsetDateTime.now(clock);
        usuarioId = UUID.randomUUID();
        negocioId = UUID.randomUUID();
        proyectoId = UUID.randomUUID();
        bloqueId = UUID.randomUUID();
        registroId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Planes HTTP')",
                usuarioId, usuarioId + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Planes HTTP', ?)",
                negocioId, usuarioId);
        suscripcionService.asignarGratuito(negocioId, ahora.minusDays(2));
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Sistema')",
                proyectoId, negocioId);
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Productos')",
                bloqueId, negocioId, proyectoId);
        jdbcTemplate.update("INSERT INTO registro (id, negocio_id, bloque_id, valores) VALUES (?, ?, ?, '{}')",
                registroId, negocioId, bloqueId);
        sesion = givenSesion();
    }

    @Test
    @DisplayName("El catálogo es público y no incluye suscripción, uso, negocio ni historial")
    void catalogoShouldBePublicWithoutBusinessData() throws Exception {
        mockMvc.perform(get("/api/planes/catalogo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("GRATUITO"))
                .andExpect(jsonPath("$[1].codigo").value("PREMIUM"))
                .andExpect(jsonPath("$[0].id").doesNotExist())
                .andExpect(jsonPath("$[0].negocioId").doesNotExist())
                .andExpect(jsonPath("$[0].suscripcion").doesNotExist())
                .andExpect(jsonPath("$[0].uso").doesNotExist())
                .andExpect(jsonPath("$[0].prueba").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/planes", "/api/planes/renovacion", "/api/planes/catalogo/privado"})
    @DisplayName("Publicar el catálogo no abre consultas privadas ni rutas vecinas")
    void privatePlansShouldRequireSession(String ruta) throws Exception {
        mockMvc.perform(get(ruta))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El catálogo no admite escrituras anónimas")
    void catalogoShouldRejectAnonymousWrite() throws Exception {
        mockMvc.perform(post("/api/planes/catalogo"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El receptor real de contenidos admite tres claves y rechaza la cuarta sin reiniciar el cupo")
    void contenidosShouldEnforceMonthlyQuotaOverHttp() throws Exception {
        mockMvc.perform(post("/api/planes/contenidos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[\"a\",\"b\",\"c\",\"d\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.autorizadas.length()").value(3))
                .andExpect(jsonPath("$.bloqueadas").value(1))
                .andExpect(header().string("Cache-Control", "no-store"));

        mockMvc.perform(post("/api/planes/contenidos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"AVISOS\",\"claves\":[\"d\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.autorizadas.length()").value(0))
                .andExpect(jsonPath("$.usadas").value(3));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM contenido_plan_desbloqueado WHERE negocio_id=?",
                Long.class, negocioId)).isEqualTo(3);
    }

    @Test
    @DisplayName("El desbloqueo exige autenticación y CSRF")
    void contenidosShouldRequireAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/planes/contenidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/planes/contenidos").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[]}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"receta", "reporte", "excel"})
    @DisplayName("El gratuito recibe 403 y un código claro al pedir una función premium")
    void obtenerShouldRejectPremiumFeatureOnFreePlan(String funcion) throws Exception {
        mockMvc.perform(get(ruta(funcion)).session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUNCION_PREMIUM"))
                .andExpect(jsonPath("$.message").value(
                        "Esta función requiere un plan premium activo. Tus datos se conservan."))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"receta", "reporte", "excel"})
    @DisplayName("Un premium activo puede consultar las funciones disponibles")
    void obtenerShouldAllowPremiumFeatureOnActivePlan(String funcion) throws Exception {
        givenPremium(ahora.plusDays(30));

        mockMvc.perform(get(ruta(funcion)).session(sesion)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una suscripción vencida pierde el acceso premium sin depender de un job")
    void obtenerShouldRejectPremiumAtExpiry() throws Exception {
        givenPremium(ahora.minusSeconds(1));

        mockMvc.perform(get(ruta("receta")).session(sesion))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUNCION_PREMIUM"));
    }

    @Test
    @DisplayName("Una receta ajena sigue respondiendo 404 antes de comprobar el plan")
    void obtenerShouldPreserveNotFoundForForeignResources() throws Exception {
        mockMvc.perform(get("/api/registros/{id}/receta", UUID.randomUUID()).session(sesion))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("La segunda alta de sistema se rechaza y la consulta básica conserva el existente")
    void crearProyectoShouldRejectQuotaButPreserveRead() throws Exception {
        mockMvc.perform(post("/api/proyectos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Segundo\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("LIMITE_SISTEMAS"));

        mockMvc.perform(get("/api/proyectos").session(sesion).param("negocioId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("El límite de filas bloquea altas pero permite editar las existentes")
    void crearRegistroShouldRejectQuotaButPreserveEditing() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 999)
                """, negocioId, bloqueId);

        mockMvc.perform(post("/api/bloques/{id}/registros", bloqueId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valores\":{}}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_REGISTROS"));

        mockMvc.perform(patch("/api/registros/{id}", registroId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valores\":{}}"))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM registro WHERE negocio_id = ?", Long.class, negocioId)).isEqualTo(1000);
    }

    @Test
    @DisplayName("La lectura inmediata no puede eludir la cuota mensual ni elegir otro negocio")
    void leerImagenShouldRejectMonthlyQuotaFromSession() throws Exception {
        givenMonthlyQuotaFull();
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", ImagenTestData.jpeg());
        MockMultipartFile pedido = new MockMultipartFile(
                "pedido", "", "application/json", PEDIDO.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).param("negocioId", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT sum(paginas) FROM consumo_ia_diario WHERE negocio_id = ?", Long.class, negocioId))
                .isEqualTo(10);
    }

    @Test
    @DisplayName("La subida rechaza una cuota mensual agotada antes de guardar archivos y lotes")
    void subirLoteShouldRejectMonthlyQuotaBeforeSavingFiles() throws Exception {
        givenMonthlyQuotaFull();
        MockMultipartFile foto = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", JPEG);

        mockMvc.perform(multipart("/api/lotes").file(foto).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM lote_subida WHERE negocio_id = ?", Long.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Un reintento de IA también respeta la cuota mensual agotada")
    void reintentarShouldRejectMonthlyQuota() throws Exception {
        givenMonthlyQuotaFull();
        UUID loteId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, motivo_error) VALUES (?, ?, 'ERROR', 'Fallo')
                """, loteId, negocioId);

        mockMvc.perform(post("/api/lotes/{id}/reintentar", loteId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));
    }

    @Test
    @DisplayName("La lectura inmediata exige sesión antes de recibir el negocio")
    void leerImagenShouldRequireSession() throws Exception {
        mockMvc.perform(multipart("/api/importaciones/lectura")
                        .file(new MockMultipartFile("imagen", new byte[] {1})))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El inicio de prueba exige una sesión autenticada")
    void iniciarPruebaShouldRequireSession() throws Exception {
        mockMvc.perform(post("/api/planes/prueba"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El inicio de prueba exige el token CSRF de la sesión")
    void iniciarPruebaShouldRequireCsrfToken() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion))
                .andExpect(status().isForbidden());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("La prueba ignora plan, fechas y negocio enviados por el cliente")
    void iniciarPruebaShouldUseSessionBusinessAndServerPolicy() throws Exception {
        UUID ajenoId = UUID.randomUUID();

        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .param("negocioId", ajenoId.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"negocioId\":\"" + ajenoId + "\",\"dias\":365,\"paginasIa\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioEn").isNotEmpty())
                .andExpect(jsonPath("$.finEn").isNotEmpty())
                .andExpect(jsonPath("$.paginasIaUtilizadas").value(0))
                .andExpect(jsonPath("$.limitePaginasIa").value(10))
                .andExpect(header().string("Cache-Control", "no-store"));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, negocioId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, ajenoId)).isZero();
    }

    @Test
    @DisplayName("Repetir el inicio devuelve una restricción clara sin renovar la prueba")
    void iniciarPruebaShouldRejectRepeatedRequest() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRUEBA_UTILIZADA"));
    }

    @Test
    @DisplayName("El consumo agotado de prueba devuelve su código propio en la ruta de IA")
    void leerImagenShouldRejectExhaustedTrialQuota() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 10 WHERE negocio_id = ?", negocioId);
        entityManager.clear();
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", ImagenTestData.jpeg());
        MockMultipartFile pedido = new MockMultipartFile(
                "pedido", "", "application/json", PEDIDO.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_PRUEBA"));
    }

    @Test
    @DisplayName("Consultar planes exige sesión")
    void consultarPlanesShouldRequireSession() throws Exception {
        mockMvc.perform(get("/api/planes")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("El gratuito consulta catálogo y uso del negocio sin activar la prueba")
    void consultarPlanesShouldReturnFreePlanAndUsageFromSession() throws Exception {
        mockMvc.perform(get("/api/planes").session(sesion).param("negocioId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.planes.length()").value(2))
                .andExpect(jsonPath("$.planes[0].codigo").value("GRATUITO"))
                .andExpect(jsonPath("$.planes[1].precioMensual").value(14900))
                .andExpect(jsonPath("$.planes[1].moneda").value("ARS"))
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("GRATUITO"))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(true))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(false))
                .andExpect(jsonPath("$.uso.proyectos").value(1))
                .andExpect(jsonPath("$.uso.registros").value(1))
                .andExpect(jsonPath("$.uso.periodoIa").value("MENSUAL"))
                .andExpect(jsonPath("$.uso.limitePaginasIa").value(10))
                .andExpect(jsonPath("$.prueba").doesNotExist());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, negocioId)).isZero();
    }

    @Test
    @DisplayName("Consultar después de activar muestra diez páginas totales y las fechas persistidas")
    void consultarPlanesShouldReflectTrialActivation() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 8 WHERE negocio_id = ?", negocioId);
        entityManager.clear();

        mockMvc.perform(get("/api/planes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("PREMIUM"))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(true))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.paginasIa").value(8))
                .andExpect(jsonPath("$.uso.periodoIa").value("PRUEBA"))
                .andExpect(jsonPath("$.uso.limitePaginasIa").value(10))
                .andExpect(jsonPath("$.prueba.paginasIaUtilizadas").value(8))
                .andExpect(jsonPath("$.prueba.finEn").isNotEmpty());
    }

    @Test
    @DisplayName("Una prueba agotada mantiene premium y no ofrece otra activación")
    void consultarPlanesShouldKeepPremiumWhenTrialPagesAreExhausted() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 10 WHERE negocio_id = ?", negocioId);
        entityManager.clear();

        mockMvc.perform(get("/api/planes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(true))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.paginasIa").value(10));
    }

    @Test
    @DisplayName("Una prueba vencida aparece gratuita sin depender del worker y conserva su historial")
    void consultarPlanesShouldFallBackBeforeExpirationWorkerRuns() throws Exception {
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken))
                .andExpect(status().isOk());
        jdbcTemplate.update("UPDATE suscripcion SET inicio_en = ?, fin_en = ? WHERE negocio_id = ?",
                ahora.minusDays(8), ahora.minusDays(1), negocioId);
        jdbcTemplate.update("UPDATE prueba_premium SET inicio_en = ?, fin_en = ? WHERE negocio_id = ?",
                ahora.minusDays(8), ahora.minusDays(1), negocioId);
        entityManager.clear();

        mockMvc.perform(get("/api/planes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("GRATUITO"))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(false))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.periodoIa").value("MENSUAL"))
                .andExpect(jsonPath("$.prueba.finEn").isNotEmpty());
    }

    @Test
    @DisplayName("Premium vigente mantiene cien páginas mensuales y no ofrece prueba")
    void consultarPlanesShouldReturnPaidPremiumWithoutOfferingTrial() throws Exception {
        givenPremium(ahora.plusDays(30));

        mockMvc.perform(get("/api/planes").session(sesion))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("PREMIUM"))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(false))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.periodoIa").value("MENSUAL"))
                .andExpect(jsonPath("$.uso.limitePaginasIa").value(100));
    }

    // --- arrange ---
    private void givenPremium(OffsetDateTime fin) {
        jdbcTemplate.update("""
                UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'PREMIUM'),
                    inicio_en = ?, fin_en = ? WHERE negocio_id = ?
                """, ahora.minusDays(1), fin, negocioId);
        entityManager.clear();
    }

    private void givenMonthlyQuotaFull() {
        jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas, reservadas) VALUES (?, ?, 10, 0)
                """, negocioId, ahora.toLocalDate());
    }

    private MockHttpSession givenSesion() throws Exception {
        MockHttpSession session = new MockHttpSession();
        var securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(usuarioId.toString(), null, List.of()));
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        new HttpSessionCsrfTokenRepository().saveToken(
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"), request, new MockHttpServletResponse());
        csrfToken = JsonPath.read(mockMvc.perform(get("/api/auth/csrf").session(session))
                .andReturn().getResponse().getContentAsString(), "$.token");
        return session;
    }

    private String ruta(String funcion) {
        return switch (funcion) {
            case "receta" -> "/api/registros/" + registroId + "/receta";
            case "reporte" -> "/api/bloques/" + bloqueId + "/reporte";
            case "excel" -> "/api/bloques/" + bloqueId + "/reporte/descarga";
            default -> throw new IllegalArgumentException(funcion);
        };
    }
}
