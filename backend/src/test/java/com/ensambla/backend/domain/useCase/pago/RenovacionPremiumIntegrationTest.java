package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import com.ensambla.backend.domain.useCase.pago.contratarPremium.ContratarPremium;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
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
import org.springframework.test.web.servlet.ResultActions;
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
    private RenovacionPremium renovacion;
    private PruebaPremiumResponse prueba;
    private ResultActions respuesta;
    private ResultActions segundaRespuesta;
    private UUID anterior;
    private String factura;
    private String recursoId;

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
    void elPrimerCobroConservaLosDiasRestantesDePruebaYTodaviaNoIniciaTiempoPago() throws Exception {
        dadoUnaPruebaIniciadaHaceTresDias();

        cuandoSePideElContrato();

        entoncesElContratoQuedaPendienteDeTreintaDiasPor14900Pesos();
        entoncesElPrimerCobroEsAlTerminarLaPrueba();
        entoncesLaSuscripcionSigueEnPrueba();
        entoncesNoHayPagosRegistrados();
    }

    @Test
    @DisplayName("Autorizar la suscripción no habilita Premium sin pago aprobado")
    void autorizarLaSuscripcionNoHabilitaPremiumSinPagoAprobado() throws Exception {
        dadoUnContratoPendiente();

        cuandoElProveedorAutorizaLaSuscripcionYSeActualiza();

        entoncesLaRespuestaNoTieneContenido();
        entoncesElPlanEs("GRATUITO");
        entoncesLaRenovacionEstaEn("AUTORIZADA");
    }

    @Test
    @DisplayName("Un pago aprobado concede sólo su período de treinta días y un duplicado no lo extiende")
    void unPagoAprobadoConcedeSoloSuPeriodoDeTreintaDiasYUnDuplicadoNoLoExtiende() throws Exception {
        dadoUnContratoConUnPagoRemoto("approved");

        cuandoSeSincronizaDosVeces();

        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(30));
        entoncesHayPagosRegistrados(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected", "cancelled"})
    @DisplayName("Un pago pendiente, rechazado o cancelado no habilita Premium")
    void unPagoPendienteRechazadoOCanceladoNoHabilitaPremium(String estado) throws Exception {
        dadoUnContratoConUnPagoRemoto(estado);

        cuandoSeSincroniza();

        entoncesElPlanEs("GRATUITO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected"})
    @DisplayName("Un siguiente cobro pendiente o rechazado conserva los días del período ya pagado")
    void unSiguienteCobroPendienteORechazadoConservaLosDiasDelPeriodoYaPagado(String estado) throws Exception {
        dadoUnPrimerPeriodoPagadoYUnSiguienteCobro(estado, AHORA.plusDays(29));

        cuandoSeSincroniza();

        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(30));
        entoncesHayPagosRegistrados(2);
    }

    @Test
    @DisplayName("Al vencer el período pagado una renovación rechazada vuelve a Gratuito y conserva los datos")
    void alVencerElPeriodoPagadoUnaRenovacionRechazadaVuelveAGratuitoYConservaLosDatos() throws Exception {
        dadoUnPrimerPeriodoPagadoUnProyectoYUnSiguienteCobroRechazado();

        cuandoSeSincroniza();

        entoncesElPlanEs("GRATUITO");
        entoncesElNegocioConservaSuProyecto();
        entoncesElPagoDeLaFacturaEstaEn("primera", "approved");
    }

    @Test
    @DisplayName("Reembolsar un período futuro conserva el período actual ya pagado")
    void reembolsarUnPeriodoFuturoConservaElPeriodoActualYaPagado() throws Exception {
        dadoDosPeriodosPagados(AHORA.plusDays(30), AHORA);

        cuandoSeReembolsaLaFactura("segunda", AHORA.plusDays(30), AHORA.plusMinutes(1));

        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(30));
        entoncesElPagoDeLaFacturaEstaEn("primera", "approved");
        entoncesElPagoDeLaFacturaEstaEn("segunda", "refunded");
    }

    @Test
    @DisplayName("Reembolsar una factura anterior no elimina el período renovado de otra factura aprobada")
    void reembolsarUnaFacturaAnteriorNoEliminaElPeriodoRenovadoDeOtraFacturaAprobada() throws Exception {
        dadoDosPeriodosPagados(AHORA.plusDays(30), AHORA.plusDays(30));

        cuandoSeReembolsaLaFactura("primera", AHORA, AHORA.plusDays(31));

        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(60));
        entoncesElPagoDeLaFacturaEstaEn("segunda", "approved");
    }

    @Test
    @DisplayName("Un reembolso completo retira sólo ese período y una entrega vieja no lo restaura")
    void unReembolsoCompletoRetiraSoloEsePeriodoYUnaEntregaViejaNoLoRestaura() throws Exception {
        dadoUnPeriodoPagadoDeUnaFacturaCualquiera();

        cuandoSeReembolsaLaFacturaYLlegaUnaEntregaVieja();

        entoncesElPlanEs("GRATUITO");
    }

    @Test
    @DisplayName("Cancelar desde Planes conserva el período ya pagado y confirma la baja con el proveedor")
    void cancelarDesdePlanesConservaElPeriodoYaPagadoYConfirmaLaBajaConElProveedor() throws Exception {
        dadoUnPeriodoPagadoDeUnaFacturaCualquiera();

        cuandoSePideCancelarLaRenovacion();

        entoncesLaRespuestaInformaUnContratoCanceladoConBajaSolicitada();
        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(30));
    }

    @Test
    @DisplayName("Cancelar durante la prueba no elimina los días gratuitos restantes")
    void cancelarDuranteLaPruebaNoEliminaLosDiasGratuitosRestantes() throws Exception {
        dadoUnaPruebaIniciadaHaceTresDias();
        dadoUnContratoPendiente();

        cuandoSePideCancelarLaRenovacion();

        entoncesLaRespuestaEsExitosa();
        entoncesLaSuscripcionSigueEnPrueba();
    }

    @Test
    @DisplayName("Una falla al cancelar conserva la intención y la conciliación recupera la baja")
    void unaFallaAlCancelarConservaLaIntencionYLaConciliacionRecuperaLaBaja() throws Exception {
        dadoUnContratoCuyaPrimeraBajaFallaEnElProveedor();

        cuandoSeIntentaCancelarYLuegoSeConcilia();

        entoncesLaRespuestaEsUnaFallaDeLaPasarela();
        entoncesLaBajaQuedaSolicitada();
        entoncesLaRenovacionEstaEn("CANCELADA");
    }

    @Test
    @DisplayName("Una respuesta de creación perdida se recupera por referencia sin crear otro contrato")
    void unaRespuestaDeCreacionPerdidaSeRecuperaPorReferenciaSinCrearOtroContrato() throws Exception {
        dadoUnProveedorQueCreaElContratoPeroPierdeLaRespuesta();

        cuandoSePideElContratoYSeReintenta();

        entoncesLaPrimeraRespuestaEsUnaFallaDeLaPasarela();
        entoncesElReintentoRecuperaElContratoSinCrearOtro();
    }

    @Test
    @DisplayName("Dos preparaciones concurrentes comparten un único contrato abierto")
    void dosPreparacionesConcurrentesCompartenUnUnicoContratoAbierto() throws Exception {
        cuandoSePreparanDosContratacionesALaVez();

        entoncesSeCreaUnSoloContratoEnElProveedor();
        entoncesHayUnaSolaRenovacionAbierta();
    }

    @Test
    @DisplayName("La consulta usa la sesión e ignora negocio e importe enviados por el navegador")
    void laConsultaUsaLaSesionEIgnoraNegocioEImporteEnviadosPorElNavegador() throws Exception {
        dadoUnContratoPendiente();

        cuandoSeConsultaLaRenovacionConUnNegocioAjeno();

        entoncesSeResponseConElContratoDeLaSesionSinCache();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/planes/renovacion", "/api/planes/renovacion/cancelar",
            "/api/planes/renovacion/actualizar"})
    @DisplayName("Las escrituras de pago exigen sesión y CSRF")
    void lasEscriturasDePagoExigenSesionYCsrf(String ruta) throws Exception {
        cuandoSeEscribeSinSesionYSinCsrf(ruta);

        entoncesSinSesionSeRechazaConUnauthorized();
        entoncesSinCsrfSeRechazaConForbidden();
    }

    @Test
    @DisplayName("La configuración incompleta no crea contratos ni habilita el cobro")
    void laConfiguracionIncompletaNoCreaContratosNiHabilitaElCobro() throws Exception {
        dadoUnaPasarelaSinConfigurar();

        cuandoSePideElContratoYSeConsultaLaRenovacion();

        entoncesSeRechazaElContratoYElCobroQuedaDeshabilitado();
        entoncesNoSeCreaNingunaRenovacion();
    }

    @Test
    @DisplayName("Una renovación cancelada no revive por una lectura posterior de autorización")
    void unaRenovacionCanceladaNoRevivePorUnaLecturaPosteriorDeAutorizacion() throws Exception {
        dadoUnContratoCanceladoPorElProveedor();

        cuandoLlegaUnaLecturaPosteriorDeAutorizacion();

        entoncesLaRenovacionEstaEn("CANCELADA");
    }

    @Test
    @DisplayName("Al vencer el período pago vuelve al gratuito sin borrar sus proyectos")
    void alVencerElPeriodoPagoVuelveAlGratuitoSinBorrarSusProyectos() throws Exception {
        dadoUnPeriodoPagadoYUnProyectoConservado();

        cuandoVenceElPeriodoPagado();

        entoncesElPlanEs("GRATUITO");
        entoncesElNegocioConservaSuProyecto();
    }

    @Test
    @DisplayName("Una contratación abierta impide iniciar después una prueba con cobro anticipado")
    void unaContratacionAbiertaImpideIniciarDespuesUnaPruebaConCobroAnticipado() throws Exception {
        dadoUnContratoPendiente();

        cuandoSeConsultanLosPlanesYSeIntentaIniciarLaPrueba();

        entoncesNoSePuedeIniciarLaPruebaPorLaRenovacionEnCurso();
        entoncesNoHayPruebasIniciadas(0);
    }

    @Test
    @DisplayName("Después de cancelar una contratación sin pagos todavía puede iniciar su única prueba")
    void despuesDeCancelarUnaContratacionSinPagosTodaviaPuedeIniciarSuUnicaPrueba() throws Exception {
        dadoUnContratoCancelado();

        cuandoSeConsultanLosPlanesYSeIniciaLaPrueba();

        entoncesSePuedeIniciarLaPrueba();
        entoncesNoHayPruebasIniciadas(1);
    }

    @Test
    @DisplayName("Una contratación nueva se elige antes que el historial cancelado aunque compartan hora")
    void unaContratacionNuevaSeEligeAntesQueElHistorialCanceladoAunqueCompartanHora() throws Exception {
        dadoUnContratoCancelado();

        cuandoSeContrataDeNuevo();

        entoncesLaRenovacionActualEsOtraYEstaPendiente();
    }

    @Test
    @DisplayName("La conciliación rota los contratos consultados incluso si no recibieron eventos")
    void laConciliacionRotaLosContratosConsultadosInclusoSiNoRecibieronEventos() throws Exception {
        dadoUnContratoPendienteConsultableUnMinutoDespues();

        cuandoSeConcilianLosPagos();

        entoncesLaRenovacionFueSincronizadaEn(AHORA.plusMinutes(1));
    }

    @Test
    @DisplayName("Cancelar dos veces no vuelve a solicitar una baja ya confirmada")
    void cancelarDosVecesNoVuelveASolicitarUnaBajaYaConfirmada() throws Exception {
        dadoUnContratoCancelado();

        cuandoSePideCancelarLaRenovacion();

        entoncesLaRespuestaInformaUnContratoCancelado();
        entoncesLaBajaSeSolicitoUnaSolaVezAlProveedor();
    }

    @Test
    @DisplayName("Cada factura cubre su ciclo y una aprobación anticipada no extiende el período actual")
    void cadaFacturaCubreSuCicloYUnaAprobacionAnticipadaNoExtiendeElPeriodoActual() throws Exception {
        dadoUnPrimerPeriodoPagado();

        cuandoLlegaLaAprobacionAnticipadaDeLaSegundaFactura();

        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(30));

        cuandoVenceElPeriodoPagado();

        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(60));
    }

    @Test
    @DisplayName("Un webhook válido concilia un contrato conocido sin requerir sesión ni CSRF")
    void unWebhookValidoConciliaUnContratoConocidoSinRequerirSesionNiCSRF() throws Exception {
        dadoUnContratoConUnPagoAprobadoYUnEventoConocido();

        cuandoLlegaElWebhookDeMercadoPagoSinSesion();

        entoncesLaRespuestaEsExitosa();
        entoncesElPlanEs("PREMIUM");
    }

    @Test
    @DisplayName("Un webhook no autenticado por firma devuelve 401 y no cambia el plan")
    void unWebhookNoAutenticadoPorFirmaDevuelve401YNoCambiaElPlan() throws Exception {
        dadoUnWebhookConFirmaInvalida();

        cuandoLlegaElWebhookDePago();

        entoncesSeRechazaConUnauthorizedSinCache();
        entoncesElPlanEs("GRATUITO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"subscription_authorized_payment", "payment"})
    @DisplayName("El aviso firmado de la segunda factura renueva una sola vez aunque llegue dos días tarde")
    void elAvisoFirmadoDeLaSegundaFacturaRenuevaUnaSolaVezAunqueLlegueDosDiasTarde(String tipo) throws Exception {
        dadoUnaSegundaFacturaConFirmaReal(tipo, "approved", AHORA.plusDays(32));

        cuandoLlegaDosVecesElAvisoFirmadoDeLaSegundaFactura(tipo);

        entoncesLosDosAvisosSeAceptan();
        entoncesElPlanEs("PREMIUM");
        entoncesElPeriodoPagadoTerminaEn(AHORA.plusDays(60));
        entoncesHayPagosRegistrados(2);
        entoncesElPagoDeLaFacturaEstaEn("segunda-firmada", "approved");
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "rejected"})
    @DisplayName("El aviso firmado de una segunda factura sin cobrar no renueva al vencer el primer período")
    void elAvisoFirmadoDeUnaSegundaFacturaSinCobrarNoRenuevaAlVencerElPrimerPeriodo(String estado) throws Exception {
        dadoUnaSegundaFacturaConFirmaReal("subscription_authorized_payment", estado, AHORA.plusDays(30));

        cuandoLlegaUnaVezElAvisoFirmadoDeLaSegundaFactura("subscription_authorized_payment");

        entoncesElPlanEs("GRATUITO");
        entoncesElPagoDeLaFacturaEstaEn("primera-firmada", "approved");
        entoncesElPagoDeLaFacturaEstaEn("segunda-firmada", estado);
    }

    // --- dado ---
    private void dadoUnContratoPendiente() throws Exception {
        contratar();
        renovacion = renovacion();
    }

    private void dadoUnaPruebaIniciadaHaceTresDias() {
        prueba = pruebaPremiumService.iniciar(negocioId, AHORA.minusDays(3));
    }

    private void dadoUnContratoConUnPagoRemoto(String estado) throws Exception {
        dadoUnContratoPendiente();
        dadoPago(pago(renovacion, UUID.randomUUID().toString(), estado, AHORA, AHORA));
    }

    private void dadoUnPrimerPeriodoPagado() throws Exception {
        dadoUnPrimerPeriodoPagadoDeLaFactura("primera");
    }

    private void dadoUnPrimerPeriodoPagadoDeLaFactura(String nombre) throws Exception {
        dadoUnContratoPendiente();
        aplicarPago(renovacion, pago(renovacion, facturaId(nombre), "approved", AHORA, AHORA), AHORA);
    }

    private void dadoUnPeriodoPagadoDeUnaFacturaCualquiera() throws Exception {
        dadoUnContratoPendiente();
        factura = UUID.randomUUID().toString();
        aplicarPago(renovacion, pago(renovacion, factura, "approved", AHORA, AHORA), AHORA);
    }

    private void dadoUnPrimerPeriodoPagadoYUnSiguienteCobro(String estado, OffsetDateTime ahora) throws Exception {
        dadoUnPrimerPeriodoPagado();
        dadoPago(pago(renovacion, facturaId("segunda"), estado, AHORA.plusDays(30), AHORA.plusDays(29)));
        when(clock.instant()).thenReturn(ahora.toInstant());
    }

    private void dadoUnPrimerPeriodoPagadoUnProyectoYUnSiguienteCobroRechazado() throws Exception {
        dadoUnPrimerPeriodoPagado();
        jdbcTemplate.update("INSERT INTO proyecto(negocio_id,nombre) VALUES (?,'Conservado tras rechazo')",
                negocioId.valor());
        dadoPago(pago(renovacion, facturaId("segunda"), "rejected", AHORA.plusDays(30), AHORA.plusDays(30)));
        when(clock.instant()).thenReturn(AHORA.plusDays(30).toInstant());
    }

    private void dadoDosPeriodosPagados(OffsetDateTime finDelSegundo, OffsetDateTime cobradoElSegundo)
            throws Exception {
        dadoUnPrimerPeriodoPagado();
        aplicarPago(renovacion, pago(renovacion, facturaId("segunda"), "approved", finDelSegundo, cobradoElSegundo),
                cobradoElSegundo);
    }

    private void dadoUnPeriodoPagadoYUnProyectoConservado() throws Exception {
        dadoUnPrimerPeriodoPagado();
        jdbcTemplate.update("INSERT INTO proyecto(negocio_id,nombre) VALUES (?,'Conservado')", negocioId.valor());
    }

    private void dadoUnContratoCanceladoPorElProveedor() throws Exception {
        dadoUnContratoPendiente();
        renovacionPremiumService.actualizarContrato(renovacion.getId(),
                contrato(renovacion, "cancelled", AHORA.plusSeconds(2)), AHORA);
    }

    private void dadoUnContratoCancelado() throws Exception {
        dadoUnContratoPendiente();
        anterior = renovacion.getId().valor();
        cancelar().andExpect(status().isOk());
    }

    private void dadoUnContratoCuyaPrimeraBajaFallaEnElProveedor() throws Exception {
        dadoUnContratoPendiente();
        doThrow(new PasarelaPagoException()).doAnswer(invocacion -> {
            remotos.put(renovacion.getProveedorId(), contrato(renovacion, "cancelled", AHORA.plusSeconds(1)));
            return null;
        }).when(pasarelaSuscripcionesPort).cancelar(anyString());
    }

    private void dadoUnProveedorQueCreaElContratoPeroPierdeLaRespuesta() {
        doAnswer(invocacion -> {
            RenovacionPremium nueva = invocacion.getArgument(0);
            ContratoRemoto remoto = contrato(nueva, "pending", AHORA);
            remotos.put(remoto.id(), remoto);
            throw new PasarelaPagoException();
        }).when(pasarelaSuscripcionesPort).crear(any(), anyString());
    }

    private void dadoUnaPasarelaSinConfigurar() {
        when(pasarelaSuscripcionesPort.disponible()).thenReturn(false);
    }

    private void dadoUnContratoPendienteConsultableUnMinutoDespues() throws Exception {
        dadoUnContratoPendiente();
        when(pasarelaSuscripcionesPort.consultar(anyString())).thenAnswer(invocacion -> Optional
                .ofNullable(remotos.get(invocacion.<String>getArgument(0))).orElseThrow(PasarelaPagoException::new));
        when(clock.instant()).thenReturn(AHORA.plusMinutes(1).toInstant());
    }

    private void dadoUnContratoConUnPagoAprobadoYUnEventoConocido() throws Exception {
        dadoUnContratoPendiente();
        when(pasarelaSuscripcionesPort.identificarContrato("subscription_preapproval", "mp-evento"))
                .thenReturn(renovacion.getProveedorId());
        dadoPago(pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA, AHORA));
    }

    private void dadoUnWebhookConFirmaInvalida() {
        doThrow(new NotificacionPagoInvalidaException()).when(pasarelaSuscripcionesPort)
                .validarNotificacion(null, null, "123");
    }

    private void dadoUnaSegundaFacturaConFirmaReal(String tipo, String estado, OffsetDateTime instante)
            throws Exception {
        dadoUnPrimerPeriodoPagadoDeLaFactura("primera-firmada");
        remotos.put(renovacion.getProveedorId(), contrato(renovacion, "authorized", AHORA.plusDays(30)));
        dadoPago(pago(renovacion, facturaId("segunda-firmada"), estado, AHORA.plusDays(30), AHORA.plusDays(30)));
        when(clock.instant()).thenReturn(instante.toInstant());
        recursoId = facturaId("segunda-firmada");
        when(pasarelaSuscripcionesPort.identificarContrato(tipo, recursoId)).thenReturn(renovacion.getProveedorId());
        dadoUnValidadorDeFirmaReal();
    }

    private void dadoUnValidadorDeFirmaReal() {
        MercadoPagoProperties propiedades = new MercadoPagoProperties();
        propiedades.setHabilitado(true);
        propiedades.setWebhookSecret(SECRETO_WEBHOOK_LOCAL);
        var validador = new MercadoPagoSuscripcionesAdapter(propiedades, new ObjectMapper(), HttpClient.newHttpClient());
        doAnswer(invocacion -> {
            validador.validarNotificacion(invocacion.getArgument(0), invocacion.getArgument(1), invocacion.getArgument(2));
            return null;
        }).when(pasarelaSuscripcionesPort).validarNotificacion(anyString(), anyString(), anyString());
    }

    private void dadoPago(PagoRemoto pago) {
        when(pasarelaSuscripcionesPort.listarPagos(pago.contratoId())).thenReturn(List.of(pago));
    }

    // --- cuando ---
    private void cuandoSePideElContrato() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf));
    }

    private void cuandoElProveedorAutorizaLaSuscripcionYSeActualiza() throws Exception {
        remotos.put(renovacion.getProveedorId(), contrato(renovacion, "authorized", AHORA.plusSeconds(1)));
        respuesta = mockMvc.perform(post("/api/planes/renovacion/actualizar").session(sesion)
                .header("X-CSRF-TOKEN", csrf));
    }

    private void cuandoSeSincroniza() {
        sincronizar(renovacion.getId());
    }

    private void cuandoSeSincronizaDosVeces() {
        sincronizar(renovacion.getId());
        sincronizar(renovacion.getId());
    }

    private void cuandoSeReembolsaLaFactura(String nombre, OffsetDateTime fin, OffsetDateTime cuando) {
        aplicarPago(renovacion, pago(renovacion, facturaId(nombre), "refunded", fin, cuando), cuando);
    }

    private void cuandoSeReembolsaLaFacturaYLlegaUnaEntregaVieja() {
        aplicarPago(renovacion, pago(renovacion, factura, "refunded", AHORA, AHORA.plusMinutes(1)),
                AHORA.plusMinutes(1));
        aplicarPago(renovacion, pago(renovacion, factura, "approved", AHORA, AHORA), AHORA);
    }

    private void cuandoSePideCancelarLaRenovacion() throws Exception {
        respuesta = cancelar();
    }

    private void cuandoSeIntentaCancelarYLuegoSeConcilia() throws Exception {
        respuesta = cancelar();
        sincronizar(renovacion().getId());
    }

    private void cuandoSePideElContratoYSeReintenta() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf));
        segundaRespuesta = mockMvc.perform(post("/api/planes/renovacion").session(sesion)
                .header("X-CSRF-TOKEN", csrf));
    }

    private void cuandoSePreparanDosContratacionesALaVez() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var primera = executor.submit(() -> contratarPremium.execute(usuarioId));
            var segunda = executor.submit(() -> contratarPremium.execute(usuarioId));
            primera.get();
            segunda.get();
        }
    }

    private void cuandoSeConsultaLaRenovacionConUnNegocioAjeno() throws Exception {
        respuesta = mockMvc.perform(get("/api/planes/renovacion").session(sesion)
                .param("negocioId", UUID.randomUUID().toString()));
    }

    private void cuandoSeEscribeSinSesionYSinCsrf(String ruta) throws Exception {
        respuesta = mockMvc.perform(post(ruta));
        segundaRespuesta = mockMvc.perform(post(ruta).session(sesion));
    }

    private void cuandoSePideElContratoYSeConsultaLaRenovacion() throws Exception {
        respuesta = mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf));
        segundaRespuesta = mockMvc.perform(get("/api/planes/renovacion").session(sesion));
    }

    private void cuandoLlegaUnaLecturaPosteriorDeAutorizacion() {
        renovacionPremiumService.actualizarContrato(renovacion.getId(),
                contrato(renovacion, "authorized", AHORA.plusSeconds(3)), AHORA);
    }

    private void cuandoVenceElPeriodoPagado() {
        recalcular(renovacion, AHORA.plusDays(30));
    }

    private void cuandoSeConsultanLosPlanesYSeIntentaIniciarLaPrueba() throws Exception {
        respuesta = mockMvc.perform(get("/api/planes").session(sesion));
        segundaRespuesta = mockMvc.perform(post("/api/planes/prueba").session(sesion).header("X-CSRF-TOKEN", csrf));
    }

    private void cuandoSeConsultanLosPlanesYSeIniciaLaPrueba() throws Exception {
        cuandoSeConsultanLosPlanesYSeIntentaIniciarLaPrueba();
    }

    private void cuandoSeContrataDeNuevo() throws Exception {
        contratar();
    }

    private void cuandoSeConcilianLosPagos() {
        conciliarPagos.execute();
    }

    private void cuandoLlegaLaAprobacionAnticipadaDeLaSegundaFactura() {
        aplicarPago(renovacion, pago(renovacion, UUID.randomUUID().toString(), "approved", AHORA.plusDays(30), AHORA),
                AHORA);
    }

    private void cuandoLlegaElWebhookDeMercadoPagoSinSesion() throws Exception {
        respuesta = mockMvc.perform(post("/api/pagos/mercadopago").param("type", "subscription_preapproval")
                .param("data.id", "mp-evento").header("x-signature", "firma").header("x-request-id", "solicitud"));
    }

    private void cuandoLlegaElWebhookDePago() throws Exception {
        respuesta = mockMvc.perform(post("/api/pagos/mercadopago").param("type", "payment").param("data.id", "123"));
    }

    private void cuandoLlegaDosVecesElAvisoFirmadoDeLaSegundaFactura(String tipo) throws Exception {
        respuesta = notificarSegundaFactura(tipo, recursoId);
        segundaRespuesta = notificarSegundaFactura(tipo, recursoId);
    }

    private void cuandoLlegaUnaVezElAvisoFirmadoDeLaSegundaFactura(String tipo) throws Exception {
        respuesta = notificarSegundaFactura(tipo, recursoId);
    }

    private ResultActions notificarSegundaFactura(String tipo, String recurso) throws Exception {
        String tiempo = Long.toString(clock.instant().getEpochSecond());
        String manifiesto = "id:" + recurso + ";request-id:" + SOLICITUD_SEGUNDA_FACTURA + ";ts:" + tiempo + ";";
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO_WEBHOOK_LOCAL.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String firma = HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
        return mockMvc.perform(post("/api/pagos/mercadopago").param("type", tipo).param("data.id", recurso)
                .header("x-signature", "ts=" + tiempo + ",v1=" + firma)
                .header("x-request-id", SOLICITUD_SEGUNDA_FACTURA));
    }

    private void contratar() throws Exception {
        mockMvc.perform(post("/api/planes/renovacion").session(sesion).header("X-CSRF-TOKEN", csrf))
                .andExpect(status().isOk());
    }

    private ResultActions cancelar() throws Exception {
        return mockMvc.perform(post("/api/planes/renovacion/cancelar").session(sesion).header("X-CSRF-TOKEN", csrf));
    }

    private void sincronizar(RenovacionId id) {
        renovacionPremiumService.sincronizar(id, OffsetDateTime.now(clock));
    }

    /** Aplica el pago como lo hace la sincronización: la pasarela lo informa y se recalcula en {@code ahora}. */
    private void aplicarPago(RenovacionPremium renovacion, PagoRemoto pago, OffsetDateTime ahora) {
        dadoPago(pago);
        when(clock.instant()).thenReturn(ahora.toInstant());
        sincronizar(renovacion.getId());
    }

    /** Sincroniza sin pagos nuevos: sólo recalcula el plan en {@code ahora}. */
    private void recalcular(RenovacionPremium renovacion, OffsetDateTime ahora) {
        when(pasarelaSuscripcionesPort.listarPagos(renovacion.getProveedorId())).thenReturn(List.of());
        when(clock.instant()).thenReturn(ahora.toInstant());
        sincronizar(renovacion.getId());
    }

    // --- entonces ---
    private void entoncesElContratoQuedaPendienteDeTreintaDiasPor14900Pesos() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.periodoDias").value(30))
                .andExpect(jsonPath("$.contrato.importe").value(14900))
                .andExpect(jsonPath("$.contrato.moneda").value("ARS"))
                .andExpect(jsonPath("$.contrato.estado").value("PENDIENTE"));
    }

    private void entoncesElPrimerCobroEsAlTerminarLaPrueba() {
        assertThat(renovacion().getPrimerCobroEn()).isEqualTo(prueba.finEn());
    }

    private void entoncesLaSuscripcionSigueEnPrueba() {
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM suscripcion WHERE negocio_id=?",
                String.class, negocioId.valor())).isEqualTo("PRUEBA");
    }

    private void entoncesNoHayPagosRegistrados() {
        entoncesHayPagosRegistrados(0);
    }

    private void entoncesHayPagosRegistrados(int cantidad) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM pago_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isEqualTo(cantidad);
    }

    private void entoncesNoHayPruebasIniciadas(int cantidad) {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM prueba_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isEqualTo(cantidad);
    }

    private void entoncesLaRespuestaNoTieneContenido() throws Exception {
        respuesta.andExpect(status().isNoContent());
    }

    private void entoncesLaRespuestaEsExitosa() throws Exception {
        respuesta.andExpect(status().isOk());
    }

    private void entoncesElPlanEs(String codigo) {
        assertThat(planActual()).isEqualTo(codigo);
    }

    private void entoncesLaRenovacionEstaEn(String estado) {
        assertThat(renovacion().getEstado()).isEqualTo(estado);
    }

    private void entoncesElPeriodoPagadoTerminaEn(OffsetDateTime fin) {
        assertThat(finPagado()).isEqualTo(fin);
    }

    private void entoncesElPagoDeLaFacturaEstaEn(String nombre, String estado) {
        assertThat(jdbcTemplate.queryForObject("SELECT estado FROM pago_premium WHERE factura_id=?",
                String.class, facturaId(nombre))).isEqualTo(estado);
    }

    private void entoncesElNegocioConservaSuProyecto() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM proyecto WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
    }

    private void entoncesLaRespuestaInformaUnContratoCanceladoConBajaSolicitada() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.contrato.estado").value("CANCELADA"))
                .andExpect(jsonPath("$.contrato.cancelacionSolicitada").value(true));
    }

    private void entoncesLaRespuestaInformaUnContratoCancelado() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.contrato.estado").value("CANCELADA"));
    }

    private void entoncesLaRespuestaEsUnaFallaDeLaPasarela() throws Exception {
        respuesta.andExpect(status().isBadGateway());
    }

    private void entoncesLaBajaQuedaSolicitada() {
        assertThat(renovacion().isCancelacionSolicitada()).isTrue();
    }

    private void entoncesLaPrimeraRespuestaEsUnaFallaDeLaPasarela() throws Exception {
        entoncesLaRespuestaEsUnaFallaDeLaPasarela();
    }

    private void entoncesElReintentoRecuperaElContratoSinCrearOtro() throws Exception {
        segundaRespuesta.andExpect(status().isOk());
        verify(pasarelaSuscripcionesPort, times(1)).crear(any(), anyString());
        assertThat(renovacion().getProveedorId()).isNotBlank();
    }

    private void entoncesSeCreaUnSoloContratoEnElProveedor() {
        verify(pasarelaSuscripcionesPort, times(1)).crear(any(), anyString());
    }

    private void entoncesHayUnaSolaRenovacionAbierta() {
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM renovacion_premium WHERE negocio_id=?",
                Integer.class, negocioId.valor())).isOne();
    }

    private void entoncesSeResponseConElContratoDeLaSesionSinCache() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.contrato.id").value(renovacion().getId().toString()));
    }

    private void entoncesSinSesionSeRechazaConUnauthorized() throws Exception {
        respuesta.andExpect(status().isUnauthorized());
    }

    private void entoncesSinCsrfSeRechazaConForbidden() throws Exception {
        segundaRespuesta.andExpect(status().isForbidden());
    }

    private void entoncesSeRechazaElContratoYElCobroQuedaDeshabilitado() throws Exception {
        respuesta.andExpect(status().isBadRequest());
        segundaRespuesta.andExpect(status().isOk()).andExpect(jsonPath("$.habilitada").value(false))
                .andExpect(jsonPath("$.contrato").doesNotExist());
    }

    private void entoncesNoSeCreaNingunaRenovacion() {
        assertThat(renovacionPremiumService.consultar(negocioId)).isEmpty();
    }

    private void entoncesNoSePuedeIniciarLaPruebaPorLaRenovacionEnCurso() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(false));
        segundaRespuesta.andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("RENOVACION_EN_CURSO"));
    }

    private void entoncesSePuedeIniciarLaPrueba() throws Exception {
        respuesta.andExpect(status().isOk()).andExpect(jsonPath("$.suscripcion.puedeIniciarPrueba").value(true));
        segundaRespuesta.andExpect(status().isOk());
    }

    private void entoncesLaRenovacionActualEsOtraYEstaPendiente() {
        assertThat(renovacion().getId()).isNotEqualTo(anterior);
        assertThat(renovacion().getEstado()).isEqualTo("PENDIENTE");
    }

    private void entoncesLaRenovacionFueSincronizadaEn(OffsetDateTime instante) {
        assertThat(renovacion().getSincronizadoEn()).isEqualTo(instante);
    }

    private void entoncesLaBajaSeSolicitoUnaSolaVezAlProveedor() {
        verify(pasarelaSuscripcionesPort, times(1)).cancelar(anyString());
    }

    private void entoncesSeRechazaConUnauthorizedSinCache() throws Exception {
        respuesta.andExpect(status().isUnauthorized()).andExpect(header().string("Cache-Control", "no-store"));
    }

    private void entoncesLosDosAvisosSeAceptan() throws Exception {
        respuesta.andExpect(status().isOk());
        segundaRespuesta.andExpect(status().isOk());
    }

    private RenovacionPremium renovacion() {
        return renovacionPremiumService.consultar(negocioId).orElseThrow();
    }

    private String facturaId(String nombre) {
        return negocioId + "-" + nombre;
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
