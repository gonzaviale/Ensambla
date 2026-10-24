package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import com.ensambla.backend.domain.useCase.pago.contratarPremium.ContratarPremium;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import com.ensambla.backend.infrastructure.pago.MercadoPagoProperties;
import com.ensambla.backend.infrastructure.pago.MercadoPagoSuscripcionesAdapter;
import com.ensambla.backend.support.PostgresRepositoryTestSupport;
import com.ensambla.backend.support.PruebasPremiumDePrueba;
import com.ensambla.backend.support.SuscripcionesDePrueba;
import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.contrato;
import static com.ensambla.backend.support.PagoTestData.pago;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RenovacionPremiumIntegrationTest extends PostgresRepositoryTestSupport {

    private static final String SECRETO_WEBHOOK_LOCAL = "secreto-exclusivo-del-test-sin-credenciales";

    private static final String SOLICITUD_SEGUNDA_FACTURA = "aviso-local-segunda-factura";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter securityFilter;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SuscripcionesDePrueba suscripcionService;

    @Autowired
    private PruebasPremiumDePrueba pruebaPremiumService;

    @Autowired
    private RenovacionPremiumService renovacionPremiumService;

    @Autowired
    private SincronizarRenovacion sincronizarRenovacion;

    @Autowired
    private ContratarPremium contratarPremium;

    @Autowired
    private ConciliarPagos conciliarPagos;

    @MockitoBean
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @MockitoBean
    private Clock clock;

    private final Map<String, ContratoRemoto> remotos = new ConcurrentHashMap<>();

    private MockMvc mockMvc;
    private MockHttpSession sesion;
    private UsuarioId usuarioId;
    private NegocioId negocioId;
    private String csrf;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(AHORA.toInstant());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        when(pasarelaSuscripcionesPort.disponible()).thenReturn(true);
        doCallRealMethod().when(pasarelaSuscripcionesPort).exigirDisponible();
        when(pasarelaSuscripcionesPort.crear(any(), anyString())).thenAnswer(invocacion -> {
            RenovacionPremium renovacion = invocacion.getArgument(0);
            ContratoRemoto remoto = contrato(renovacion, "pending", AHORA);
            remotos.put(remoto.id(), remoto);
            return remoto;
        });
        when(pasarelaSuscripcionesPort.consultar(anyString())).thenAnswer(invocacion ->
                remotos.get(invocacion.getArgument(0)));
        when(pasarelaSuscripcionesPort.buscar(any())).thenAnswer(invocacion -> remotos.values().stream()
                .filter(remoto -> remoto.referencia().equals(invocacion.getArgument(0))).findFirst());
        when(pasarelaSuscripcionesPort.listarPagos(anyString())).thenReturn(List.of());
        doAnswer(invocacion -> {
            String id = invocacion.getArgument(0);
            ContratoRemoto anterior = remotos.get(id);
            RenovacionPremium renovacion = renovacionPremiumService.obtener(anterior.referencia());
            remotos.put(id, contrato(renovacion, "cancelled", AHORA.plusSeconds(1)));
            return null;
        }).when(pasarelaSuscripcionesPort).cancelar(anyString());
        usuarioId = UsuarioId.nuevo();
        negocioId = NegocioId.nuevo();
        jdbcTemplate.update("INSERT INTO usuario (id,email,nombre) VALUES (?,?,'Pagos HTTP')",
                usuarioId.valor(), usuarioId.valor() + "@example.test");
        jdbcTemplate.update("INSERT INTO negocio (id,nombre,duenio_id) VALUES (?,'Pagos HTTP',?)",
                negocioId.valor(), usuarioId.valor());
        suscripcionService.asignarGratuito(negocioId, AHORA);
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(securityFilter).build();
        sesion = new MockHttpSession();
        var seguridad = SecurityContextHolder.createEmptyContext();
        seguridad.setAuthentication(new UsernamePasswordAuthenticationToken(usuarioId.toString(), null, List.of()));
        sesion.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, seguridad);
        try {
            String respuesta = mockMvc.perform(get("/api/auth/csrf").session(sesion))
                    .andReturn().getResponse().getContentAsString();
            csrf = JsonPath.read(respuesta, "$.token");
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    @DisplayName("El primer cobro conserva los días restantes de prueba y todavía no inicia tiempo pago")
    void contratarShouldPreserveTrialUntilFirstApprovedPayment() throws Exception {
        var prueba = pruebaPremiumService.iniciar(negocioId, AHORA.minusDays(3));

        mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.periodoDias").value(30))
                .andExpect(jsonPath("$.contrato.importe").value(14900))
                .andExpect(jsonPath("$.contrato.moneda").value("ARS"))
                .andExpect(jsonPath("$.contrato.estado").value("PENDIENTE"));

        RenovacionPremium renovacion = renovacion();
        assertThat(renovacion.getPrimerCobroEn()).isEqualTo(prueba.finEn());
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM suscripcion WHERE negocio_id=?",
                String.class, negocioId.valor())).isEqualTo("PRUEBA");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pago_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isZero();
    }

    @Test
    @DisplayName("Autorizar la suscripción no habilita Premium sin pago aprobado")
    void actualizarShouldNotGrantPremiumForAuthorizationOnly() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        remotos.put(renovacion.getProveedorId(), contrato(renovacion, "authorized", AHORA.plusSeconds(1)));

        mockMvc.perform(post("/api/planes/renovacion/actualizar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isNoContent());

        assertThat(planActual()).isEqualTo("GRATUITO");
        assertThat(renovacion().getEstado()).isEqualTo("AUTORIZADA");
    }

    @Test
    @DisplayName("Un pago aprobado concede sólo su período de treinta días y un duplicado no lo extiende")
    void actualizarShouldApplyApprovedInvoiceOnce() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        givenPago(pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA));

        sincronizarRenovacion.execute(renovacion.getId());
        sincronizarRenovacion.execute(renovacion.getId());

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(30));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pago_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected", "cancelled"})
    @DisplayName("Un pago pendiente, rechazado o cancelado no habilita Premium")
    void actualizarShouldNotGrantAccessForUnpaidInvoice(String estado) throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        givenPago(pago(renovacion, UUID.randomUUID().toString(), estado, AHORA, AHORA));

        sincronizarRenovacion.execute(renovacion.getId());

        assertThat(planActual()).isEqualTo("GRATUITO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected"})
    @DisplayName("Un siguiente cobro pendiente o rechazado conserva los días del período ya pagado")
    void actualizarShouldPreservePaidPeriodWhenRenewalIsUnpaid(String estado) throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera"), "approved", AHORA, AHORA), AHORA);
        givenPago(pago(renovacion, facturaId("segunda"), estado, AHORA.plusDays(30), AHORA.plusDays(29)));
        when(clock.instant()).thenReturn(AHORA.plusDays(29).toInstant());

        sincronizarRenovacion.execute(renovacion.getId());

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(30));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pago_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isEqualTo(2);
    }

    @Test
    @DisplayName("Al vencer el período pagado una renovación rechazada vuelve a Gratuito y conserva los datos")
    void actualizarShouldExpireAccessWhenNextInvoiceIsRejected() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera"), "approved", AHORA, AHORA), AHORA);
        jdbcTemplate.update("INSERT INTO proyecto(negocio_id,nombre) VALUES (?,'Conservado tras rechazo')",
                negocioId.valor());
        givenPago(pago(renovacion, facturaId("segunda"), "rejected", AHORA.plusDays(30), AHORA.plusDays(30)));
        when(clock.instant()).thenReturn(AHORA.plusDays(30).toInstant());

        sincronizarRenovacion.execute(renovacion.getId());

        assertThat(planActual()).isEqualTo("GRATUITO");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM proyecto WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("primera"))).isEqualTo("approved");
    }

    @Test
    @DisplayName("Reembolsar un período futuro conserva el período actual ya pagado")
    void aplicarPagoShouldKeepCurrentPeriodWhenFutureInvoiceIsRefunded() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera"), "approved", AHORA, AHORA), AHORA);
        aplicarPago(renovacion,
                pago(renovacion, facturaId("segunda"), "approved", AHORA.plusDays(30), AHORA), AHORA);

        aplicarPago(renovacion,
                pago(renovacion, facturaId("segunda"), "refunded", AHORA.plusDays(30), AHORA.plusMinutes(1)),
                AHORA.plusMinutes(1));

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(30));
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("primera"))).isEqualTo("approved");
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("segunda"))).isEqualTo("refunded");
    }

    @Test
    @DisplayName("Reembolsar una factura anterior no elimina el período renovado de otra factura aprobada")
    void aplicarPagoShouldKeepRenewedPeriodWhenEarlierInvoiceIsRefunded() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera"), "approved", AHORA, AHORA), AHORA);
        aplicarPago(renovacion,
                pago(renovacion, facturaId("segunda"), "approved", AHORA.plusDays(30), AHORA.plusDays(30)),
                AHORA.plusDays(30));

        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera"), "refunded", AHORA, AHORA.plusDays(31)), AHORA.plusDays(31));

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(60));
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("segunda"))).isEqualTo("approved");
    }

    @Test
    @DisplayName("Un reembolso completo retira sólo ese período y una entrega vieja no lo restaura")
    void actualizarShouldRevokeRefundedPeriodAndIgnoreOlderApproval() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        String factura = UUID.randomUUID().toString();
        aplicarPago(renovacion,
                pago(renovacion, factura, "approved", AHORA, AHORA), AHORA);

        aplicarPago(renovacion,
                pago(renovacion, factura, "refunded", AHORA, AHORA.plusMinutes(1)), AHORA.plusMinutes(1));
        aplicarPago(renovacion,
                pago(renovacion, factura, "approved", AHORA, AHORA), AHORA);

        assertThat(planActual()).isEqualTo("GRATUITO");
    }

    @Test
    @DisplayName("Cancelar desde Planes conserva el período ya pagado y confirma la baja con el proveedor")
    void cancelarShouldPreservePaidPeriod() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA), AHORA);

        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contrato.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.contrato.cancelacionSolicitada").value(true));

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(30));
    }

    @Test
    @DisplayName("Cancelar durante la prueba no elimina los días gratuitos restantes")
    void cancelarShouldPreserveTrial() throws Exception {
        pruebaPremiumService.iniciar(negocioId, AHORA.minusDays(3));
        givenContrato();

        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM suscripcion WHERE negocio_id=?",
                String.class, negocioId.valor())).isEqualTo("PRUEBA");
    }

    @Test
    @DisplayName("Una falla al cancelar conserva la intención y la conciliación recupera la baja")
    void cancelarShouldRetryPendingCancellation() throws Exception {
        givenContrato();
        doThrow(new PasarelaPagoException()).doAnswer(invocacion -> {
            RenovacionPremium renovacion = renovacion();
            remotos.put(renovacion.getProveedorId(), contrato(renovacion, "cancelled", AHORA.plusSeconds(1)));
            return null;
        }).when(pasarelaSuscripcionesPort).cancelar(anyString());

        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isBadGateway());
        sincronizarRenovacion.execute(renovacion().getId());

        assertThat(renovacion().isCancelacionSolicitada()).isTrue();
        assertThat(renovacion().getEstado()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Una respuesta de creación perdida se recupera por referencia sin crear otro contrato")
    void contratarShouldRecoverUncertainCreationWithoutDuplicate() throws Exception {
        doAnswer(invocacion -> {
            RenovacionPremium renovacion = invocacion.getArgument(0);
            ContratoRemoto remoto = contrato(renovacion, "pending", AHORA);
            remotos.put(remoto.id(), remoto);
            throw new PasarelaPagoException();
        }).when(pasarelaSuscripcionesPort).crear(any(), anyString());

        mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isBadGateway());
        givenContrato();

        verify(pasarelaSuscripcionesPort, times(1)).crear(any(), anyString());
        assertThat(renovacion().getProveedorId()).isNotBlank();
    }

    @Test
    @DisplayName("Dos preparaciones concurrentes comparten un único contrato abierto")
    void contratarShouldSerializeConcurrentRequests() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primera = executor.submit(() -> contratarPremium.execute(usuarioId));
            var segunda = executor.submit(() -> contratarPremium.execute(usuarioId));

            primera.get();
            segunda.get();

            verify(pasarelaSuscripcionesPort, times(1)).crear(any(), anyString());
            assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM renovacion_premium WHERE negocio_id=?",
                    Integer.class, negocioId.valor())).isOne();
        }
    }

    @Test
    @DisplayName("La consulta usa la sesión e ignora negocio e importe enviados por el navegador")
    void consultarShouldIsolateBusinessAndDisableHttpCache() throws Exception {
        givenContrato();

        mockMvc.perform(get("/api/planes/renovacion").session(sesion).param("negocioId", UUID.randomUUID().toString()))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.contrato.id").value(renovacion().getId().toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/planes/renovacion", "/api/planes/renovacion/cancelar",
            "/api/planes/renovacion/actualizar"})
    @DisplayName("Las escrituras de pago exigen sesión y CSRF")
    void pagosShouldRequireSessionAndCsrf(String ruta) throws Exception {
        mockMvc.perform(post(ruta)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ruta).session(sesion)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("La configuración incompleta no crea contratos ni habilita el cobro")
    void contratarShouldNotPersistWhenDisabled() throws Exception {
        when(pasarelaSuscripcionesPort.disponible()).thenReturn(false);

        mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/planes/renovacion").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.habilitada").value(false)).andExpect(jsonPath("$.contrato").doesNotExist());

        assertThat(renovacionPremiumService.consultar(negocioId)).isEmpty();
    }

    @Test
    @DisplayName("Una renovación cancelada no revive por una lectura posterior de autorización")
    void actualizarShouldKeepCancellationTerminal() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        renovacionPremiumService.actualizarContrato(renovacion.getId(),
                contrato(renovacion, "cancelled", AHORA.plusSeconds(2)), AHORA);

        renovacionPremiumService.actualizarContrato(renovacion.getId(),
                contrato(renovacion, "authorized", AHORA.plusSeconds(3)), AHORA);

        assertThat(renovacion().getEstado()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("Al vencer el período pago vuelve al gratuito sin borrar sus proyectos")
    void recalcularShouldExpirePaidPeriodWithoutDataLoss() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA), AHORA);
        jdbcTemplate.update("INSERT INTO proyecto(negocio_id,nombre) VALUES (?,'Conservado')", negocioId.valor());

        recalcular(renovacion, AHORA.plusDays(30));

        assertThat(planActual()).isEqualTo("GRATUITO");
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM proyecto WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
    }

    private void givenContrato() throws Exception {
        mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Una contratación abierta impide iniciar después una prueba con cobro anticipado")
    void iniciarPruebaShouldRejectWhilePaymentAgreementIsOpen() throws Exception {
        givenContrato();

        mockMvc.perform(get("/api/planes").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false));
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("RENOVACION_EN_CURSO"));

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isZero();
    }

    @Test
    @DisplayName("Después de cancelar una contratación sin pagos todavía puede iniciar su única prueba")
    void iniciarPruebaShouldAllowAfterUnpaidContractCancellation() throws Exception {
        givenContrato();
        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/planes").session(sesion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(true));
        mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
    }

    @Test
    @DisplayName("Una contratación nueva se elige antes que el historial cancelado aunque compartan hora")
    void consultarShouldPreferOpenContractWithEqualCreationTimestamp() throws Exception {
        givenContrato();
        UUID anterior = renovacion().getId().valor();
        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());

        givenContrato();

        assertThat(renovacion().getId()).isNotEqualTo(anterior);
        assertThat(renovacion().getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    @DisplayName("La conciliación rota los contratos consultados incluso si no recibieron eventos")
    void conciliarShouldPersistFairReconciliationOrder() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();

        when(pasarelaSuscripcionesPort.consultar(anyString())).thenAnswer(invocacion -> Optional
                .ofNullable(remotos.get(invocacion.<String>getArgument(0))).orElseThrow(PasarelaPagoException::new));
        when(clock.instant()).thenReturn(AHORA.plusMinutes(1).toInstant());

        conciliarPagos.execute();

        assertThat(renovacion().getSincronizadoEn()).isEqualTo(AHORA.plusMinutes(1));
    }

    @Test
    @DisplayName("Cancelar dos veces no vuelve a solicitar una baja ya confirmada")
    void cancelarShouldNotResendConfirmedCancellation() throws Exception {
        givenContrato();
        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk()).andExpect(jsonPath("$.contrato.estado").value("CANCELADA"));

        verify(pasarelaSuscripcionesPort, times(1)).cancelar(anyString());
    }

    @Test
    @DisplayName("Cada factura cubre su ciclo y una aprobación anticipada no extiende el período actual")
    void aplicarPagoShouldKeepInvoicePeriodsIndependent() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA), AHORA);

        aplicarPago(renovacion,
                pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA.plusDays(30), AHORA), AHORA);
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(30));
        recalcular(renovacion, AHORA.plusDays(30));

        assertThat(finPagado()).isEqualTo(AHORA.plusDays(60));
    }

    @Test
    @DisplayName("Un webhook válido concilia un contrato conocido sin requerir sesión ni CSRF")
    void notificarShouldReconcileKnownContractWithoutUserSession() throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        when(pasarelaSuscripcionesPort.identificarContrato("subscription_preapproval", "mp-evento"))
                .thenReturn(renovacion.getProveedorId());
        givenPago(pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA));

        mockMvc.perform(post("/api/pagos/mercadopago").param("type", "subscription_preapproval")
                        .param("data.id", "mp-evento").header("x-signature", "firma")
                        .header("x-request-id", "solicitud"))
                .andExpect(status().isOk());

        assertThat(planActual()).isEqualTo("PREMIUM");
    }

    @Test
    @DisplayName("Un webhook no autenticado por firma devuelve 401 y no cambia el plan")
    void notificarShouldRejectInvalidSignature() throws Exception {
        doThrow(new NotificacionPagoInvalidaException()).when(pasarelaSuscripcionesPort)
                .validarNotificacion(null, null, "123");

        mockMvc.perform(post("/api/pagos/mercadopago").param("type", "payment").param("data.id", "123"))
                .andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));

        assertThat(planActual()).isEqualTo("GRATUITO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"subscription_authorized_payment", "payment"})
    @DisplayName("El aviso firmado de la segunda factura renueva una sola vez aunque llegue dos días tarde")
    void notificarShouldRenewOnceWhenSecondApprovedInvoiceArrivesLate(String tipo) throws Exception {
        String recursoId = givenSecondInvoiceWithRealSignatureValidation(tipo, "approved", AHORA.plusDays(32));

        notificarSegundaFactura(tipo, recursoId);
        notificarSegundaFactura(tipo, recursoId);

        assertThat(planActual()).isEqualTo("PREMIUM");
        assertThat(finPagado()).isEqualTo(AHORA.plusDays(60));
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pago_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("segunda-firmada"))).isEqualTo("approved");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected"})
    @DisplayName("El aviso firmado de una segunda factura sin cobrar no renueva al vencer el primer período")
    void notificarShouldReturnToFreeWhenSecondInvoiceRemainsUnpaid(String estado) throws Exception {
        String recursoId = givenSecondInvoiceWithRealSignatureValidation(
                "subscription_authorized_payment", estado, AHORA.plusDays(30));

        notificarSegundaFactura("subscription_authorized_payment", recursoId);

        assertThat(planActual()).isEqualTo("GRATUITO");
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("primera-firmada"))).isEqualTo("approved");
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId("segunda-firmada"))).isEqualTo(estado);
    }

    // --- arrange ---

    private String givenSecondInvoiceWithRealSignatureValidation(String tipo, String estado,
                                                                 OffsetDateTime instante) throws Exception {
        givenContrato();
        RenovacionPremium renovacion = renovacion();
        aplicarPago(renovacion,
                pago(renovacion, facturaId("primera-firmada"), "approved", AHORA, AHORA), AHORA);
        remotos.put(renovacion.getProveedorId(), contrato(renovacion, "authorized", AHORA.plusDays(30)));
        givenPago(pago(renovacion, facturaId("segunda-firmada"), estado, AHORA.plusDays(30), AHORA.plusDays(30)));
        when(clock.instant()).thenReturn(instante.toInstant());
        String recursoId = facturaId("segunda-firmada");
        when(pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId)).thenReturn(renovacion.getProveedorId());
        MercadoPagoProperties propiedades = new MercadoPagoProperties();
        propiedades.setHabilitado(true);
        propiedades.setWebhookSecret(SECRETO_WEBHOOK_LOCAL);
        var validador = new MercadoPagoSuscripcionesAdapter(propiedades, new ObjectMapper(), HttpClient.newHttpClient());
        doAnswer(invocacion -> {
            validador.validarNotificacion(invocacion.getArgument(0), invocacion.getArgument(1), invocacion.getArgument(2));
            return null;
        }).when(pasarelaSuscripcionesPort).validarNotificacion(anyString(), anyString(), anyString());
        return recursoId;
    }

    // --- act ---

    private void notificarSegundaFactura(String tipo, String recursoId) throws Exception {
        String tiempo = Long.toString(clock.instant().getEpochSecond());
        String manifiesto = "id:" + recursoId + ";request-id:" + SOLICITUD_SEGUNDA_FACTURA + ";ts:" + tiempo + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO_WEBHOOK_LOCAL.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/pagos/mercadopago").param("type", tipo).param("data.id", recursoId)
                        .header("x-signature", "ts=" + tiempo + ",v1=" + firma)
                        .header("x-request-id", SOLICITUD_SEGUNDA_FACTURA))
                .andExpect(status().isOk());
    }

    /** Aplica el pago como lo hace la sincronización: la pasarela lo informa y se recalcula en {@code ahora}. */
    private void aplicarPago(RenovacionPremium renovacion, PagoRemoto pago, OffsetDateTime ahora) {
        givenPago(pago);
        when(clock.instant()).thenReturn(ahora.toInstant());
        sincronizarRenovacion.execute(renovacion.getId());
    }

    /** Sincroniza sin pagos nuevos: sólo recalcula el plan en {@code ahora}. */
    private void recalcular(RenovacionPremium renovacion, OffsetDateTime ahora) {
        when(pasarelaSuscripcionesPort.listarPagos(renovacion.getProveedorId())).thenReturn(List.of());
        when(clock.instant()).thenReturn(ahora.toInstant());
        sincronizarRenovacion.execute(renovacion.getId());
    }

    private RenovacionPremium renovacion() {
        return renovacionPremiumService.consultar(negocioId).orElseThrow();
    }

    private String facturaId(String nombre) {
        return negocioId + "-" + nombre;
    }

    private void givenPago(PagoRemoto pago) {
        when(pasarelaSuscripcionesPort.listarPagos(pago.contratoId())).thenReturn(List.of(pago));
    }

    private String planActual() {
        return jdbcTemplate.queryForObject("SELECT p.codigo FROM suscripcion s JOIN plan p ON p.id=s.plan_id "
                + "WHERE s.negocio_id=?", String.class, negocioId.valor());
    }

    private OffsetDateTime finPagado() {
        return jdbcTemplate.queryForObject("SELECT fin_en FROM suscripcion WHERE negocio_id=?",
                OffsetDateTime.class, negocioId.valor());
    }
}
