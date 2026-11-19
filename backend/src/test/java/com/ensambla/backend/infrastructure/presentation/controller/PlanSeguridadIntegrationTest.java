package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.support.ImagenTestData;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.SuscripcionesDePrueba;
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
import org.springframework.test.web.servlet.ResultActions;
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
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private Clock clock;

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private String csrfToken;
    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private ProyectoId proyectoId;
    private BloqueId bloqueId;
    private RegistroId registroId;
    private OffsetDateTime ahora;

    private ResultActions respuesta;
    private ResultActions segundaRespuesta;
    private UUID ajenoId;
    private LoteId loteId;
    private MockMultipartFile foto;
    private MockMultipartFile imagen;
    private MockMultipartFile pedido;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        ahora = OffsetDateTime.now(clock);
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        proyectoId = ProyectoId.nuevo();
        bloqueId = BloqueId.nuevo();
        registroId = RegistroId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id, email, nombre) VALUES (?, ?, 'Planes HTTP')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id, nombre, duenio_id) VALUES (?, 'Planes HTTP', ?)",
                negocioId.valor(), usuarioId.valor());
        suscripcionService.asignarGratuito(negocioId, ahora.minusDays(2));
        jdbcTemplate.update("INSERT INTO proyecto (id, negocio_id, nombre) VALUES (?, ?, 'Sistema')",
                proyectoId.valor(), negocioId.valor());
        jdbcTemplate.update("INSERT INTO bloque (id, negocio_id, proyecto_id, nombre) VALUES (?, ?, ?, 'Productos')",
                bloqueId.valor(), negocioId.valor(), proyectoId.valor());
        jdbcTemplate.update("INSERT INTO registro (id, negocio_id, bloque_id, valores) VALUES (?, ?, ?, '{}')",
                registroId.valor(), negocioId.valor(), bloqueId.valor());
        sesion = dadoSesion();
    }

    @Test
    @DisplayName("El receptor real de contenidos admite seis claves y rechaza la séptima sin reiniciar el cupo")
    void elReceptorRealDeContenidosAdmiteSeisClavesYRechazaLaSeptimaSinReiniciarElCupo() throws Exception {
        cuandoSeEjecutaElReceptorRealDeContenidosAdmiteSeisClavesYRechazaLaSeptimaSinReiniciarElCupo();

        entoncesSeVerificaElReceptorRealDeContenidosAdmiteSeisClavesYRechazaLaSeptimaSinReiniciarElCupo();
    }

    @Test
    @DisplayName("El desbloqueo exige autenticación y CSRF")
    void elDesbloqueoExigeAutenticacionYCSRF() throws Exception {
        cuandoSeEjecutaElDesbloqueoExigeAutenticacionYCSRF();

        entoncesSeVerificaElDesbloqueoExigeAutenticacionYCSRF();
    }

    @ParameterizedTest
    @ValueSource(strings = {"receta", "reporte", "excel"})
    @DisplayName("El gratuito recibe 403 y un código claro al pedir una función premium")
    void elGratuitoRecibe403YUnCodigoClaroAlPedirUnaFuncionPremium(String funcion) throws Exception {
        cuandoSePideLaFuncion(funcion);

        entoncesSeRechazaConUnCodigoClaroDePlanPremium();
    }

    @ParameterizedTest
    @ValueSource(strings = {"receta", "reporte", "excel"})
    @DisplayName("Un premium activo puede consultar las funciones disponibles")
    void unPremiumActivoPuedeConsultarLasFuncionesDisponibles(String funcion) throws Exception {
        dadoPremium(ahora.plusDays(30));

        cuandoSePideLaFuncion(funcion);

        entoncesLaFuncionEstaDisponible();
    }

    @Test
    @DisplayName("Una suscripción vencida pierde el acceso premium sin depender de un job")
    void unaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob() throws Exception {
        dadoElEscenarioDeUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob();

        cuandoSeEjecutaUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob();

        entoncesSeVerificaUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob();
    }

    @Test
    @DisplayName("Una receta ajena sigue respondiendo 404 antes de comprobar el plan")
    void unaRecetaAjenaSigueRespondiendo404AntesDeComprobarElPlan() throws Exception {
        cuandoSeEjecutaUnaRecetaAjenaSigueRespondiendo404AntesDeComprobarElPlan();

        entoncesSeVerificaUnaRecetaAjenaSigueRespondiendo404AntesDeComprobarElPlan();
    }

    @Test
    @DisplayName("La segunda alta de sistema se rechaza y la consulta básica conserva el existente")
    void laSegundaAltaDeSistemaSeRechazaYLaConsultaBasicaConservaElExistente() throws Exception {
        cuandoSeEjecutaLaSegundaAltaDeSistemaSeRechazaYLaConsultaBasicaConservaElExistente();

        entoncesSeVerificaLaSegundaAltaDeSistemaSeRechazaYLaConsultaBasicaConservaElExistente();
    }

    @Test
    @DisplayName("El límite de filas bloquea altas pero permite editar las existentes")
    void elLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes() throws Exception {
        dadoElEscenarioDeElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes();

        cuandoSeEjecutaElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes();

        entoncesSeVerificaElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes();
    }

    @Test
    @DisplayName("La lectura inmediata no puede eludir la cuota mensual ni elegir otro negocio")
    void laLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio() throws Exception {
        dadoElEscenarioDeLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio();

        cuandoSeEjecutaLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio();

        entoncesSeVerificaLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio();
    }

    @Test
    @DisplayName("La subida rechaza una cuota mensual agotada antes de guardar archivos y lotes")
    void laSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes() throws Exception {
        dadoElEscenarioDeLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes();

        cuandoSeEjecutaLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes();

        entoncesSeVerificaLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes();
    }

    @Test
    @DisplayName("Un reintento de IA también respeta la cuota mensual agotada")
    void unReintentoDeIATambienRespetaLaCuotaMensualAgotada() throws Exception {
        dadoElEscenarioDeUnReintentoDeIATambienRespetaLaCuotaMensualAgotada();

        cuandoSeEjecutaUnReintentoDeIATambienRespetaLaCuotaMensualAgotada();

        entoncesSeVerificaUnReintentoDeIATambienRespetaLaCuotaMensualAgotada();
    }

    @Test
    @DisplayName("La lectura inmediata exige sesión antes de recibir el negocio")
    void laLecturaInmediataExigeSesionAntesDeRecibirElNegocio() throws Exception {
        cuandoSeEjecutaLaLecturaInmediataExigeSesionAntesDeRecibirElNegocio();

        entoncesSeVerificaLaLecturaInmediataExigeSesionAntesDeRecibirElNegocio();
    }

    @Test
    @DisplayName("El inicio de prueba exige una sesión autenticada")
    void elInicioDePruebaExigeUnaSesionAutenticada() throws Exception {
        cuandoSeEjecutaElInicioDePruebaExigeUnaSesionAutenticada();

        entoncesSeVerificaElInicioDePruebaExigeUnaSesionAutenticada();
    }

    @Test
    @DisplayName("El inicio de prueba exige el token CSRF de la sesión")
    void elInicioDePruebaExigeElTokenCSRFDeLaSesion() throws Exception {
        cuandoSeEjecutaElInicioDePruebaExigeElTokenCSRFDeLaSesion();

        entoncesSeVerificaElInicioDePruebaExigeElTokenCSRFDeLaSesion();
    }

    @Test
    @DisplayName("La prueba ignora plan, fechas y negocio enviados por el cliente")
    void laPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente() throws Exception {
        dadoElEscenarioDeLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente();

        cuandoSeEjecutaLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente();

        entoncesSeVerificaLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente();
    }

    @Test
    @DisplayName("Repetir el inicio devuelve una restricción clara sin renovar la prueba")
    void repetirElInicioDevuelveUnaRestriccionClaraSinRenovarLaPrueba() throws Exception {
        cuandoSeEjecutaRepetirElInicioDevuelveUnaRestriccionClaraSinRenovarLaPrueba();

        entoncesSeVerificaRepetirElInicioDevuelveUnaRestriccionClaraSinRenovarLaPrueba();
    }

    @Test
    @DisplayName("El consumo agotado de prueba devuelve su código propio en la ruta de IA")
    void elConsumoAgotadoDePruebaDevuelveSuCodigoPropioEnLaRutaDeIA() throws Exception {
        cuandoSeEjecutaElConsumoAgotadoDePruebaDevuelveSuCodigoPropioEnLaRutaDeIA();

        entoncesSeVerificaElConsumoAgotadoDePruebaDevuelveSuCodigoPropioEnLaRutaDeIA();
    }

    @Test
    @DisplayName("Consultar planes exige sesión")
    void consultarPlanesExigeSesion() throws Exception {
        cuandoSeEjecutaConsultarPlanesExigeSesion();

        entoncesSeVerificaConsultarPlanesExigeSesion();
    }

    @Test
    @DisplayName("El gratuito consulta catálogo y uso del negocio sin activar la prueba")
    void elGratuitoConsultaCatalogoYUsoDelNegocioSinActivarLaPrueba() throws Exception {
        cuandoSeEjecutaElGratuitoConsultaCatalogoYUsoDelNegocioSinActivarLaPrueba();

        entoncesSeVerificaElGratuitoConsultaCatalogoYUsoDelNegocioSinActivarLaPrueba();
    }

    @Test
    @DisplayName("Consultar después de activar muestra diez páginas totales y las fechas persistidas")
    void consultarDespuesDeActivarMuestraDiezPaginasTotalesYLasFechasPersistidas() throws Exception {
        cuandoSeEjecutaConsultarDespuesDeActivarMuestraDiezPaginasTotalesYLasFechasPersistidas();

        entoncesSeVerificaConsultarDespuesDeActivarMuestraDiezPaginasTotalesYLasFechasPersistidas();
    }

    @Test
    @DisplayName("Una prueba agotada mantiene premium y no ofrece otra activación")
    void unaPruebaAgotadaMantienePremiumYNoOfreceOtraActivacion() throws Exception {
        cuandoSeEjecutaUnaPruebaAgotadaMantienePremiumYNoOfreceOtraActivacion();

        entoncesSeVerificaUnaPruebaAgotadaMantienePremiumYNoOfreceOtraActivacion();
    }

    @Test
    @DisplayName("Una prueba vencida aparece gratuita sin depender del worker y conserva su historial")
    void unaPruebaVencidaApareceGratuitaSinDependerDelWorkerYConservaSuHistorial() throws Exception {
        cuandoSeEjecutaUnaPruebaVencidaApareceGratuitaSinDependerDelWorkerYConservaSuHistorial();

        entoncesSeVerificaUnaPruebaVencidaApareceGratuitaSinDependerDelWorkerYConservaSuHistorial();
    }

    @Test
    @DisplayName("Premium vigente mantiene cien páginas mensuales y no ofrece prueba")
    void premiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba() throws Exception {
        dadoElEscenarioDePremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba();

        cuandoSeEjecutaPremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba();

        entoncesSeVerificaPremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba();
    }

    // --- dado ---
    private void dadoPremium(OffsetDateTime fin) {
        jdbcTemplate.update("""
                UPDATE suscripcion SET plan_id = (SELECT id FROM plan WHERE codigo = 'PREMIUM'),
                    inicio_en = ?, fin_en = ? WHERE negocio_id = ?
                """, ahora.minusDays(1), fin, negocioId.valor());
        entityManager.clear();
    }

    private void dadoMonthlyQuotaFull() {
        jdbcTemplate.update("""
                INSERT INTO consumo_ia_diario (negocio_id, fecha, paginas, reservadas) VALUES (?, ?, 10, 0)
                """, negocioId.valor(), ZonaNegocio.dia(ahora));
    }

    private MockHttpSession dadoSesion() throws Exception {
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

    // --- dado ---

    private void dadoElEscenarioDeUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob() throws Exception {
        dadoPremium(ahora.minusSeconds(1));
    }

    private void dadoElEscenarioDeElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes() throws Exception {
        jdbcTemplate.update("""
                INSERT INTO registro (negocio_id, bloque_id, valores)
                SELECT ?, ?, '{}'::jsonb FROM generate_series(1, 999)
                """, negocioId.valor(), bloqueId.valor());
    }

    private void dadoElEscenarioDeLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio() throws Exception {
        dadoMonthlyQuotaFull();
        imagen = new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", ImagenTestData.jpeg());
        pedido = new MockMultipartFile(
                "pedido", "", "application/json", PEDIDO.getBytes(StandardCharsets.UTF_8));
    }

    private void dadoElEscenarioDeLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes() throws Exception {
        dadoMonthlyQuotaFull();
        foto = new MockMultipartFile("fotos", "foto.jpg", "image/jpeg", JPEG);
    }

    private void dadoElEscenarioDeUnReintentoDeIATambienRespetaLaCuotaMensualAgotada() throws Exception {
        dadoMonthlyQuotaFull();
        loteId = LoteId.nuevo();
        jdbcTemplate.update("""
                INSERT INTO lote_subida (id, negocio_id, estado, motivo_error) VALUES (?, ?, 'ERROR', 'Fallo')
                """, loteId.valor(), negocioId.valor());
    }

    private void dadoElEscenarioDeLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente() throws Exception {
        ajenoId = UUID.randomUUID();
    }

    private void dadoElEscenarioDePremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba() throws Exception {
        dadoPremium(ahora.plusDays(30));
    }

    // --- cuando ---

    private void cuandoSeEjecutaElReceptorRealDeContenidosAdmiteSeisClavesYRechazaLaSeptimaSinReiniciarElCupo() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/contenidos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\"]}"));
        segundaRespuesta = mockMvc.perform(post("/api/planes/contenidos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"tipo\":\"AVISOS\",\"claves\":[\"g\"]}"));
    }

    private void cuandoSeEjecutaElDesbloqueoExigeAutenticacionYCSRF() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/contenidos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[]}"));
        segundaRespuesta = mockMvc.perform(post("/api/planes/contenidos").session(sesion).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"AVISOS\",\"claves\":[]}"));
    }

    private void cuandoSeEjecutaUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob() throws Exception {
        respuesta = mockMvc.perform(get(ruta("receta")).session(sesion));
    }

    private void cuandoSeEjecutaUnaRecetaAjenaSigueRespondiendo404AntesDeComprobarElPlan() throws Exception {
        respuesta = mockMvc.perform(get("/api/registros/{id}/receta", UUID.randomUUID()).session(sesion));
    }

    private void cuandoSeEjecutaLaSegundaAltaDeSistemaSeRechazaYLaConsultaBasicaConservaElExistente() throws Exception {
        respuesta = mockMvc.perform(post("/api/proyectos").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Segundo\"}"));
        segundaRespuesta = mockMvc.perform(get("/api/proyectos").session(sesion).param("negocioId", UUID.randomUUID().toString()));
    }

    private void cuandoSeEjecutaElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes() throws Exception {
        respuesta = mockMvc.perform(post("/api/bloques/{id}/registros", bloqueId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valores\":{}}"));
        segundaRespuesta = mockMvc.perform(patch("/api/registros/{id}", registroId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"valores\":{}}"));
    }

    private void cuandoSeEjecutaLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio() throws Exception {
        respuesta = mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken).param("negocioId", UUID.randomUUID().toString()));
    }

    private void cuandoSeEjecutaLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes() throws Exception {
        respuesta = mockMvc.perform(multipart("/api/lotes").file(foto).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaUnReintentoDeIATambienRespetaLaCuotaMensualAgotada() throws Exception {
        respuesta = mockMvc.perform(post("/api/lotes/{id}/reintentar", loteId).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaLaLecturaInmediataExigeSesionAntesDeRecibirElNegocio() throws Exception {
        respuesta = mockMvc.perform(multipart("/api/importaciones/lectura")
                        .file(new MockMultipartFile("imagen", new byte[] {1})));
    }

    private void cuandoSeEjecutaElInicioDePruebaExigeUnaSesionAutenticada() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba"));
    }

    private void cuandoSeEjecutaElInicioDePruebaExigeElTokenCSRFDeLaSesion() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion));
    }

    private void cuandoSeEjecutaLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken)
                        .param("negocioId", ajenoId.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"negocioId\":\"" + ajenoId + "\",\"dias\":365,\"paginasIa\":999}"));
    }

    private void cuandoSeEjecutaRepetirElInicioDevuelveUnaRestriccionClaraSinRenovarLaPrueba() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
        segundaRespuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaElConsumoAgotadoDePruebaDevuelveSuCodigoPropioEnLaRutaDeIA() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 10 WHERE negocio_id = ?", negocioId.valor());
        entityManager.clear();
        MockMultipartFile imagen = new MockMultipartFile("imagen", "foto.jpg", "image/jpeg", ImagenTestData.jpeg());
        MockMultipartFile pedido = new MockMultipartFile(
                "pedido", "", "application/json", PEDIDO.getBytes(StandardCharsets.UTF_8));
        segundaRespuesta = mockMvc.perform(multipart("/api/importaciones/lectura").file(imagen).file(pedido).session(sesion)
                        .header("X-CSRF-TOKEN", csrfToken));
    }

    private void cuandoSeEjecutaConsultarPlanesExigeSesion() throws Exception {
        respuesta = mockMvc.perform(get("/api/planes"));
    }

    private void cuandoSeEjecutaElGratuitoConsultaCatalogoYUsoDelNegocioSinActivarLaPrueba() throws Exception {
        respuesta = mockMvc.perform(get("/api/planes").session(sesion).param("negocioId", UUID.randomUUID().toString()));
    }

    private void cuandoSeEjecutaConsultarDespuesDeActivarMuestraDiezPaginasTotalesYLasFechasPersistidas() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 8 WHERE negocio_id = ?", negocioId.valor());
        entityManager.clear();
        segundaRespuesta = mockMvc.perform(get("/api/planes").session(sesion));
    }

    private void cuandoSeEjecutaUnaPruebaAgotadaMantienePremiumYNoOfreceOtraActivacion() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
        jdbcTemplate.update("UPDATE prueba_premium SET paginas_ia = 10 WHERE negocio_id = ?", negocioId.valor());
        entityManager.clear();
        segundaRespuesta = mockMvc.perform(get("/api/planes").session(sesion));
    }

    private void cuandoSeEjecutaUnaPruebaVencidaApareceGratuitaSinDependerDelWorkerYConservaSuHistorial() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrfToken));
        jdbcTemplate.update("UPDATE suscripcion SET inicio_en = ?, fin_en = ? WHERE negocio_id = ?",
                ahora.minusDays(8), ahora.minusDays(1), negocioId.valor());
        jdbcTemplate.update("UPDATE prueba_premium SET inicio_en = ?, fin_en = ? WHERE negocio_id = ?",
                ahora.minusDays(8), ahora.minusDays(1), negocioId.valor());
        entityManager.clear();
        segundaRespuesta = mockMvc.perform(get("/api/planes").session(sesion));
    }

    private void cuandoSeEjecutaPremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba() throws Exception {
        respuesta = mockMvc.perform(get("/api/planes").session(sesion));
    }

    private void cuandoSePideLaFuncion(String funcion) throws Exception {
        respuesta = mockMvc.perform(get(ruta(funcion)).session(sesion));
    }

    // --- entonces ---

    private void entoncesSeVerificaElReceptorRealDeContenidosAdmiteSeisClavesYRechazaLaSeptimaSinReiniciarElCupo() throws Exception {
        respuesta
                .andExpect(status().isOk()).andExpect(jsonPath("$.autorizadas.length()").value(6))
                .andExpect(jsonPath("$.bloqueadas").value(1))
                .andExpect(header().string("Cache-Control", "no-store"));
        segundaRespuesta
                .andExpect(status().isOk()).andExpect(jsonPath("$.autorizadas.length()").value(0))
                .andExpect(jsonPath("$.usadas").value(6));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM contenido_plan_desbloqueado WHERE negocio_id=?",
                Long.class, negocioId.valor())).isEqualTo(6);
    }

    private void entoncesSeVerificaElDesbloqueoExigeAutenticacionYCSRF() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
        segundaRespuesta
                .andExpect(status().isForbidden());
    }

    private void entoncesSeVerificaUnaSuscripcionVencidaPierdeElAccesoPremiumSinDependerDeUnJob() throws Exception {
        respuesta
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUNCION_PREMIUM"));
    }

    private void entoncesSeVerificaUnaRecetaAjenaSigueRespondiendo404AntesDeComprobarElPlan() throws Exception {
        respuesta
                .andExpect(status().isNotFound());
    }

    private void entoncesSeVerificaLaSegundaAltaDeSistemaSeRechazaYLaConsultaBasicaConservaElExistente() throws Exception {
        respuesta
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("LIMITE_SISTEMAS"));
        segundaRespuesta
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
    }

    private void entoncesSeVerificaElLimiteDeFilasBloqueaAltasPeroPermiteEditarLasExistentes() throws Exception {
        respuesta
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_REGISTROS"));
        segundaRespuesta
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM registro WHERE negocio_id = ?", Long.class, negocioId.valor())).isEqualTo(1000);
    }

    private void entoncesSeVerificaLaLecturaInmediataNoPuedeEludirLaCuotaMensualNiElegirOtroNegocio() throws Exception {
        respuesta
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT sum(paginas) FROM consumo_ia_diario WHERE negocio_id = ?", Long.class, negocioId.valor()))
                .isEqualTo(10);
    }

    private void entoncesSeVerificaLaSubidaRechazaUnaCuotaMensualAgotadaAntesDeGuardarArchivosYLotes() throws Exception {
        respuesta
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM lote_subida WHERE negocio_id = ?", Long.class, negocioId.valor())).isZero();
    }

    private void entoncesSeVerificaUnReintentoDeIATambienRespetaLaCuotaMensualAgotada() throws Exception {
        respuesta
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_MENSUAL"));
    }

    private void entoncesSeVerificaLaLecturaInmediataExigeSesionAntesDeRecibirElNegocio() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
    }

    private void entoncesSeVerificaElInicioDePruebaExigeUnaSesionAutenticada() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
    }

    private void entoncesSeVerificaElInicioDePruebaExigeElTokenCSRFDeLaSesion() throws Exception {
        respuesta
                .andExpect(status().isForbidden());
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, negocioId.valor())).isZero();
    }

    private void entoncesSeVerificaLaPruebaIgnoraPlanFechasYNegocioEnviadosPorElCliente() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicioEn").isNotEmpty())
                .andExpect(jsonPath("$.finEn").isNotEmpty())
                .andExpect(jsonPath("$.paginasIaUtilizadas").value(0))
                .andExpect(jsonPath("$.limitePaginasIa").value(10))
                .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, negocioId.valor())).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id = ?",
                Long.class, ajenoId)).isZero();
    }

    private void entoncesSeVerificaRepetirElInicioDevuelveUnaRestriccionClaraSinRenovarLaPrueba() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("PRUEBA_UTILIZADA"));
    }

    private void entoncesSeVerificaElConsumoAgotadoDePruebaDevuelveSuCodigoPropioEnLaRutaDeIA() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("LIMITE_IA_PRUEBA"));
    }

    private void entoncesSeVerificaConsultarPlanesExigeSesion() throws Exception {
        respuesta
                .andExpect(status().isUnauthorized());
    }

    private void entoncesSeVerificaElGratuitoConsultaCatalogoYUsoDelNegocioSinActivarLaPrueba() throws Exception {
        respuesta
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
                Long.class, negocioId.valor())).isZero();
    }

    private void entoncesSeVerificaConsultarDespuesDeActivarMuestraDiezPaginasTotalesYLasFechasPersistidas() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
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

    private void entoncesSeVerificaUnaPruebaAgotadaMantienePremiumYNoOfreceOtraActivacion() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(true))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.paginasIa").value(10));
    }

    private void entoncesSeVerificaUnaPruebaVencidaApareceGratuitaSinDependerDelWorkerYConservaSuHistorial() throws Exception {
        respuesta
                .andExpect(status().isOk());
        segundaRespuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("GRATUITO"))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(false))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.periodoIa").value("MENSUAL"))
                .andExpect(jsonPath("$.prueba.finEn").isNotEmpty());
    }

    private void entoncesSeVerificaPremiumVigenteMantieneCienPaginasMensualesYNoOfrecePrueba() throws Exception {
        respuesta
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.codigoPlanActual").value("PREMIUM"))
                .andExpect(jsonPath("$.suscripcion.pruebaActiva").value(false))
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false))
                .andExpect(jsonPath("$.uso.periodoIa").value("MENSUAL"))
                .andExpect(jsonPath("$.uso.limitePaginasIa").value(100));
    }

    private void entoncesSeRechazaConUnCodigoClaroDePlanPremium() throws Exception {
        respuesta.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("FUNCION_PREMIUM"))
                .andExpect(jsonPath("$.message").value(
                        "Esta función requiere un plan premium activo. Tus datos se conservan."))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    private void entoncesLaFuncionEstaDisponible() throws Exception {
        respuesta.andExpect(status().isOk());
    }
}
