package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MercadoPagoSuscripcionesAdapterTest {

    private static final String SECRETO = "secreto-exclusivamente-de-prueba";
    private static final String FIRMA_TIEMPO = "1792497600";
    private static final ObjectMapper JSON = new ObjectMapper();

    private MercadoPagoProperties properties;
    private HttpClient cliente;
    private HttpResponse<String> respuesta;
    private MercadoPagoSuscripcionesAdapter adapter;
    private final Map<String, Object> respuestas = new HashMap<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        properties = propiedades();
        cliente = mock(HttpClient.class);
        respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(200);
        when(respuesta.body()).thenAnswer(invocacion -> JSON.writeValueAsString(contratoJson()));
        when(cliente.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenAnswer(invocacion -> {
            HttpRequest request = invocacion.getArgument(0);
            Object cuerpo = respuestas.get(request.method() + " " + request.uri().getRawPath()
                    + (request.uri().getRawQuery() == null ? "" : "?" + request.uri().getRawQuery()));
            if (cuerpo != null) {
                HttpResponse<String> individual = mock(HttpResponse.class);
                when(individual.statusCode()).thenReturn(200);
                when(individual.body()).thenReturn(JSON.writeValueAsString(cuerpo));
                return individual;
            }
            return respuesta;
        });
        adapter = new MercadoPagoSuscripcionesAdapter(properties, JSON, cliente);
    }

    @Test
    @DisplayName("Crea un checkout pendiente con precio del servidor y fecha exacta, sin enviar una tarjeta")
    void crearShouldSendApprovedServerConditions() throws Exception {
        ContratoRemoto resultado = adapter.crear(renovacion(), "cliente@example.test");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(cliente).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        JsonNode body = JSON.readTree(contenido(request));
        assertThat(request.uri().toString()).isEqualTo("https://api.mercadopago.com/preapproval");
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer token-de-prueba");
        assertThat(request.timeout()).contains(Duration.ofSeconds(5));
        assertThat(body.path("external_reference").asString()).isEqualTo(RENOVACION_ID.toString());
        assertThat(body.path("payer_email").asString()).isEqualTo("cliente@example.test");
        assertThat(body.path("status").asString()).isEqualTo("pending");
        assertThat(body.path("auto_recurring").path("frequency").asInt()).isEqualTo(30);
        assertThat(body.path("auto_recurring").path("frequency_type").asString()).isEqualTo("days");
        assertThat(body.path("auto_recurring").path("transaction_amount").decimalValue()).isEqualByComparingTo("14900");
        assertThat(body.path("auto_recurring").path("currency_id").asString()).isEqualTo("ARS");
        assertThat(body.path("auto_recurring").path("start_date").asString())
                .isEqualTo("2026-10-20T12:00:00.000Z");
        assertThat(body.has("card_token_id")).isFalse();
        assertThat(resultado.referencia()).isEqualTo(RENOVACION_ID);
        assertThat(resultado.importe()).isEqualByComparingTo("14900");
        assertThat(resultado.inicioEn()).isEqualTo(AHORA);
        assertThat(resultado.proximoCobroEn()).isEqualTo(AHORA.plusDays(30));
    }

    @ParameterizedTest
    @ValueSource(ints = {199, 300, 401, 429, 500})
    @DisplayName("Una respuesta HTTP no exitosa nunca se interpreta como contratación aprobada")
    void consultarShouldRejectUnsuccessfulHttpStatus(int status) {
        when(respuesta.statusCode()).thenReturn(status);

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Una falla de red conserva un mensaje seguro sin exponer el token")
    void consultarShouldTranslateNetworkFailure() throws Exception {
        doThrow(new IOException("token privado" )).when(cliente).send(any(), any());

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class)
                .hasMessageNotContaining("token privado");
    }

    @Test
    @DisplayName("Una interrupción restaura la marca del hilo y no concede acceso")
    void consultarShouldPreserveInterruption() throws Exception {
        doThrow(new InterruptedException()).when(cliente).send(any(), any());

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class);

        assertThat(Thread.interrupted()).isTrue();
    }

    @Test
    @DisplayName("JSON inválido no permite deducir un pago exitoso")
    void consultarShouldRejectMalformedJson() {
        when(respuesta.body()).thenReturn("no es json");

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @CsvSource({"collector_id,otro", "application_id,otra", "external_reference,invalida",
            "last_modified,invalida", "id,''", "init_point,https://otro.test/checkout"})
    @DisplayName("Rechaza contratos ajenos, incompletos o con una redirección externa")
    void consultarShouldValidateRemoteContract(String campo, String valor) {
        Map<String, Object> contrato = contratoJson();
        contrato.put(campo, valor);
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Acepta un contrato sin enlace y sin próxima fecha mientras se confirma la baja")
    void consultarShouldAllowMissingOptionalFields() {
        Map<String, Object> contrato = contratoJson();
        contrato.remove("init_point");
        contrato.remove("next_payment_date");
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        ContratoRemoto resultado = adapter.consultar("mp-contrato");

        assertThat(resultado.enlace()).isEmpty();
        assertThat(resultado.proximoCobroEn()).isNull();
    }

    @Test
    @DisplayName("Acepta próxima fecha nula al cerrar la renovación")
    void consultarShouldAllowNullNextPayment() {
        Map<String, Object> contrato = contratoJson();
        contrato.put("next_payment_date", null);
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        assertThat(adapter.consultar("mp-contrato").proximoCobroEn()).isNull();
    }

    @Test
    @DisplayName("Un importe remoto malformado no se convierte silenciosamente en cero")
    void consultarShouldRejectMalformedAmount() {
        Map<String, Object> contrato = contratoJson();
        Map<String, Object> recurrencia = new HashMap<>(recurrencia());
        recurrencia.put("transaction_amount", "no es un importe");
        contrato.put("auto_recurring", recurrencia);
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        assertThatThrownBy(() -> adapter.consultar("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("La búsqueda por referencia recupera un contrato único consultando sus datos completos")
    void buscarShouldRecoverUniqueContract() {
        respuestas.put("GET /preapproval/search?q=" + RENOVACION_ID + "&limit=2",
                Map.of("results", List.of(Map.of("id", "mp-contrato"))));

        assertThat(adapter.buscar(RENOVACION_ID)).isPresent();
    }

    @Test
    @DisplayName("Una búsqueda sin contrato no provoca otra creación")
    void buscarShouldReturnEmptyWithoutContract() {
        respuestas.put("GET /preapproval/search?q=" + RENOVACION_ID + "&limit=2", Map.of("results", List.of()));

        assertThat(adapter.buscar(RENOVACION_ID)).isEmpty();
    }

    @Test
    @DisplayName("Dos contratos con la misma referencia requieren revisión en vez de elegir uno arbitrario")
    void buscarShouldRejectAmbiguousReference() {
        respuestas.put("GET /preapproval/search?q=" + RENOVACION_ID + "&limit=2",
                Map.of("results", List.of(Map.of("id", "uno"), Map.of("id", "dos"))));

        assertThatThrownBy(() -> adapter.buscar(RENOVACION_ID)).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("La baja se solicita al identificador del proveedor sin cobrar otra vez")
    void cancelarShouldRequestProviderCancellation() throws Exception {
        adapter.cancelar("mp-contrato");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(cliente).send(captor.capture(), any());
        assertThat(captor.getValue().method()).isEqualTo("PUT");
        assertThat(JSON.readTree(contenido(captor.getValue())).path("status").asString()).isEqualTo("cancelled");
    }

    @Test
    @DisplayName("Consulta cada pago real de las facturas y conserva sus fechas y estado verificados")
    void listarPagosShouldVerifyInvoicesAndPaymentDetails() {
        givenFacturas(List.of(facturaJson()));
        respuestas.put("GET /v1/payments/123", pagoJson());

        List<PagoRemoto> resultado = adapter.listarPagos("mp-contrato");

        assertThat(resultado).hasSize(1);
        PagoRemoto pago = resultado.getFirst();
        assertThat(pago.facturaId()).isEqualTo("456");
        assertThat(pago.pagoId()).isEqualTo("123");
        assertThat(pago.contratoId()).isEqualTo("mp-contrato");
        assertThat(pago.referencia()).isEqualTo(RENOVACION_ID);
        assertThat(pago.importe()).isEqualByComparingTo("14900");
        assertThat(pago.moneda()).isEqualTo("ARS");
        assertThat(pago.estado()).isEqualTo("approved");
        assertThat(pago.debitoEn()).isEqualTo(AHORA);
        assertThat(pago.modificadoEn()).isEqualTo(AHORA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approved", "rejected"})
    @DisplayName("Reconoce el formato observado de Suscripciones verificando contrato, factura y vendedor de prueba")
    void listarPagosShouldReadCurrentSubscriptionPaymentFormat(String estado) {
        givenCurrentSubscriptionPayment(estado);

        List<PagoRemoto> resultado = adapter.listarPagos("mp-contrato");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().facturaId()).isEqualTo("factura-recurrente");
        assertThat(resultado.getFirst().pagoId()).isEqualTo("123");
        assertThat(resultado.getFirst().estado()).isEqualTo(estado);
        assertThat(resultado.getFirst().referencia()).isEqualTo(RENOVACION_ID);
        assertThat(resultado.getFirst().importe()).isEqualByComparingTo("14900");
        assertThat(resultado.getFirst().debitoEn()).isEqualTo(AHORA);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " "})
    @DisplayName("Sin correo registra el rechazo sólo cuando el comprador coincide con el contrato de prueba")
    void listarPagosShouldReadRejectedTestPaymentWithoutEmail(String correo) {
        givenRejectedSubscriptionWithoutEmail(correo);

        List<PagoRemoto> resultado = adapter.listarPagos("mp-contrato");

        assertThat(resultado).hasSize(1);
        assertThat(resultado.getFirst().estado()).isEqualTo("rejected");
        assertThat(resultado.getFirst().facturaId()).isEqualTo("factura-recurrente");
        assertThat(resultado.getFirst().referencia()).isEqualTo(RENOVACION_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approved", "pending", "refunded", ""})
    @DisplayName("La excepción de correo ausente nunca admite un pago aprobado, pendiente o reembolsado")
    void listarPagosShouldRejectOtherStatesWithoutTestBuyerEmail(String estado) {
        givenRejectedSubscriptionWithoutEmail("ausente");
        Map<String, Object> pago = rejectedSubscriptionWithoutEmail("ausente");
        pago.put("status", estado);
        respuestas.put("GET /v1/payments/123", pago);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " ", "otro"})
    @DisplayName("Sin correo rechaza un comprador ausente o diferente al de la suscripción")
    void listarPagosShouldRejectUnlinkedBuyerWithoutEmail(String comprador) {
        givenRejectedSubscriptionWithoutEmail("ausente");
        Map<String, Object> pago = rejectedSubscriptionWithoutEmail("ausente");
        pago.put("payer", compradorSinCorreo(comprador));
        respuestas.put("GET /v1/payments/123", pago);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " "})
    @DisplayName("Sin correo exige que la suscripción identifique al comprador")
    void listarPagosShouldRejectMissingContractBuyerWithoutEmail(String comprador) {
        givenRejectedSubscriptionWithoutEmail("ausente");
        Map<String, Object> contrato = contratoJson();
        givenNullableField(contrato, "payer_id", comprador);
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Un correo real no se trata como correo ausente para aceptar un rechazo de prueba")
    void listarPagosShouldRejectRealBuyerEmailOnRejectedTestPayment() {
        givenRejectedSubscriptionWithoutEmail("persona@example.com");

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("Sin correo sigue exigiendo al vendedor configurado y su etiqueta de cuenta de prueba")
    void listarPagosShouldRejectUnverifiedSellerWithoutBuyerEmail(int caso) {
        givenRejectedSubscriptionWithoutEmail("ausente");
        respuestas.put("GET /users/me", caso == 0
                ? Map.of("id", 999, "tags", List.of("test_user"))
                : Map.of("id", 100, "tags", List.of("normal")));

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12})
    @DisplayName("El formato nuevo rechaza identidades contradictorias, facturas ambiguas y cuentas no verificadas")
    void listarPagosShouldRejectUnverifiedCurrentSubscriptionPayment(int caso) {
        givenInvalidCurrentSubscriptionPayment(caso);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"factura-recurrente", "456"})
    @DisplayName("Conserva la identidad de factura vinculada al pago aunque la búsqueda incluya un id público distinto")
    void listarPagosShouldKeepStablePaymentInvoiceIdentity(String idPublico) {
        givenCurrentSubscriptionPayment("approved");
        Map<String, Object> factura = facturaJson();
        factura.put("id", idPublico);
        givenFacturas(List.of(factura));

        assertThat(adapter.listarPagos("mp-contrato").getFirst().facturaId()).isEqualTo("factura-recurrente");
    }

    @Test
    @DisplayName("Un identificador de aplicación nulo usa el contrato y la suscripción vinculada al pago")
    void listarPagosShouldVerifyContractWhenPaymentApplicationIsNull() {
        givenCurrentSubscriptionPayment("approved");
        Map<String, Object> pago = currentSubscriptionPayment("approved");
        pago.put("application_id", null);
        respuestas.put("GET /v1/payments/123", pago);

        assertThat(adapter.listarPagos("mp-contrato")).hasSize(1);
    }

    @Test
    @DisplayName("Un contrato de otra referencia no respalda un pago aunque la factura y el pago coincidan")
    void listarPagosShouldRejectForeignParentContract() {
        givenFacturas(List.of(facturaJson()));
        respuestas.put("GET /v1/payments/123", pagoJson());
        Map<String, Object> contrato = contratoJson();
        contrato.put("external_reference", "00000000-0000-0000-0000-000000000000");
        respuestas.put("GET /preapproval/mp-contrato", contrato);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("El modo productivo admite un pago live verificado sin depender de una cuenta de prueba")
    void listarPagosShouldAllowLivePaymentInProductionMode() {
        properties.setProduccion(true);
        givenFacturas(List.of(facturaJson()));
        Map<String, Object> pago = pagoJson();
        pago.put("live_mode", true);
        respuestas.put("GET /v1/payments/123", pago);

        assertThat(adapter.listarPagos("mp-contrato")).hasSize(1);
    }

    @Test
    @DisplayName("El modo productivo no activa acceso con un pago marcado como no live")
    void listarPagosShouldRejectTestPaymentInProductionMode() {
        properties.setProduccion(true);
        givenFacturas(List.of(facturaJson()));
        respuestas.put("GET /v1/payments/123", pagoJson());

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @ParameterizedTest
    @CsvSource({"collector_id,otro", "application_id,otra", "external_reference,00000000-0000-0000-0000-000000000000",
            "transaction_amount,1", "currency_id,USD", "id,otro"})
    @DisplayName("Un pago con cuenta, referencia, importe, moneda o identidad diferentes no es válido")
    void listarPagosShouldRejectMismatchedPayment(String campo, String valor) {
        givenFacturas(List.of(facturaJson()));
        Map<String, Object> pago = pagoJson();
        pago.put(campo, valor);
        respuestas.put("GET /v1/payments/123", pago);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("El entorno de pruebas rechaza pagos reales aunque estén aprobados")
    void listarPagosShouldRejectLivePaymentInTestMode() {
        givenFacturas(List.of(facturaJson()));
        Map<String, Object> pago = pagoJson();
        pago.put("live_mode", true);
        respuestas.put("GET /v1/payments/123", pago);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Un pago sin ambiente explícito no es aceptado")
    void listarPagosShouldRejectUnknownEnvironment() {
        givenFacturas(List.of(facturaJson()));
        Map<String, Object> pago = pagoJson();
        pago.remove("live_mode");
        respuestas.put("GET /v1/payments/123", pago);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Una factura de otra suscripción no se aplica al contrato consultado")
    void listarPagosShouldRejectForeignInvoice() {
        Map<String, Object> factura = facturaJson();
        factura.put("preapproval_id", "otro");
        givenFacturas(List.of(factura));
        respuestas.put("GET /v1/payments/123", pagoJson());

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Las facturas todavía sin pago no habilitan acceso")
    void listarPagosShouldSkipInvoicesWithoutPayment() {
        Map<String, Object> sinId = facturaJson();
        sinId.put("payment", Map.of());
        Map<String, Object> nulo = facturaJson();
        Map<String, Object> pagoNulo = new HashMap<>();
        pagoNulo.put("id", null);
        nulo.put("payment", pagoNulo);
        givenFacturas(List.of(sinId, nulo));

        assertThat(adapter.listarPagos("mp-contrato")).isEmpty();
    }

    @Test
    @DisplayName("Recorre todas las páginas sin imponer un límite rechazado por Mercado Pago")
    void listarPagosShouldReadAllPages() {
        respuestas.put("GET /authorized_payments/search?preapproval_id=mp-contrato&offset=0",
                Map.of("paging", Map.of("total", 2), "results", List.of(facturaJson())));
        respuestas.put("GET /authorized_payments/search?preapproval_id=mp-contrato&offset=1",
                Map.of("paging", Map.of("total", 2), "results", List.of(facturaJson())));
        respuestas.put("GET /v1/payments/123", pagoJson());

        assertThat(adapter.listarPagos("mp-contrato")).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"results\":[]}", "{\"results\":{},\"paging\":{\"total\":0}}",
            "{\"results\":[],\"paging\":{\"total\":1}}"})
    @DisplayName("Una paginación incompleta o inválida no se interpreta como todos los pagos conciliados")
    void listarPagosShouldRejectMalformedPagination(String json) {
        when(respuesta.body()).thenReturn(json);

        assertThatThrownBy(() -> adapter.listarPagos("mp-contrato")).isInstanceOf(PasarelaPagoException.class);
    }

    @Test
    @DisplayName("Sin facturas devuelve una lista vacía")
    void listarPagosShouldAllowEmptyInvoices() {
        givenFacturas(List.of());

        assertThat(adapter.listarPagos("mp-contrato")).isEmpty();
    }

    @Test
    @DisplayName("La notificación sólo identifica un contrato; su cuerpo no decide el estado premium")
    void identificarContratoShouldResolveSupportedTopics() {
        respuestas.put("GET /authorized_payments/456", facturaJson());
        respuestas.put("GET /authorized_payments/search?payment_id=123", Map.of("results", List.of(facturaJson())));

        assertThat(adapter.identificarContrato("subscription_preapproval", "mp-contrato")).isEqualTo("mp-contrato");
        assertThat(adapter.identificarContrato("subscription_authorized_payment", "456")).isEqualTo("mp-contrato");
        assertThat(adapter.identificarContrato("payment", "123")).isEqualTo("mp-contrato");
        assertThat(adapter.identificarContrato("desconocido", "123")).isEmpty();
    }

    @Test
    @DisplayName("Un pago sin factura vinculada no se asigna a un negocio por aproximación")
    void identificarContratoShouldIgnoreUnlinkedPayment() {
        respuestas.put("GET /authorized_payments/search?payment_id=123", Map.of("results", List.of()));

        assertThat(adapter.identificarContrato("payment", "123")).isEmpty();
    }

    @Test
    @DisplayName("Un identificador que intenta cambiar la URL es rechazado antes de llamar a la API")
    void consultarShouldRejectUrlInjection() {
        assertThatThrownBy(() -> adapter.consultar("../otro?token=secreto"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Una firma HMAC válida autentica la notificación")
    void validarNotificacionShouldAcceptMatchingHmac() throws Exception {
        adapter.validarNotificacion(firma(), "solicitud", "123");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6})
    @DisplayName("Las firmas ausentes, malformadas o alteradas se rechazan")
    void validarNotificacionShouldRejectInvalidSignature(int caso) throws Exception {
        String firma = firma();
        String solicitud = "solicitud";
        String id = "123";
        switch (caso) {
            case 0 -> firma = null;
            case 1 -> solicitud = null;
            case 2 -> id = null;
            case 3 -> properties.setWebhookSecret("");
            case 4 -> firma = "basura,ts=no-numerico,v1=zz";
            case 5 -> firma = "ts=" + FIRMA_TIEMPO + ",v1=no-valida";
            case 6 -> id = "456";
            default -> throw new IllegalArgumentException();
        }
        String recibida = firma;
        String solicitudRecibida = solicitud;
        String recursoRecibido = id;

        assertThatThrownBy(() -> adapter.validarNotificacion(recibida, solicitudRecibida, recursoRecibido))
                .isInstanceOf(NotificacionPagoInvalidaException.class);
    }

    @Test
    @DisplayName("Una falla criptográfica nunca acepta la notificación")
    void validarNotificacionShouldRejectCryptographicFailure() throws Exception {
        String firma = firma();
        try (var estatico = mockStatic(Mac.class)) {
            estatico.when(() -> Mac.getInstance("HmacSHA256")).thenThrow(new NoSuchAlgorithmException());

            assertThatThrownBy(() -> adapter.validarNotificacion(firma, "solicitud", "123"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("Sin configuración el adaptador informa que la contratación aún no está disponible")
    void exigirDisponibleShouldRejectDisabledConfiguration() {
        properties.setHabilitado(false);

        assertThat(adapter.disponible()).isFalse();
        assertThatThrownBy(adapter::exigirDisponible).isInstanceOf(BusinessRuleException.class);
    }

    private void givenFacturas(List<Map<String, Object>> facturas) {
        respuestas.put("GET /authorized_payments/search?preapproval_id=mp-contrato&offset=0",
                Map.of("paging", Map.of("total", facturas.size()), "results", facturas));
    }

    private void givenCurrentSubscriptionPayment(String estado) {
        Map<String, Object> factura = facturaJson();
        factura.remove("id");
        givenFacturas(List.of(factura));
        respuestas.put("GET /v1/payments/123", currentSubscriptionPayment(estado));
        respuestas.put("GET /users/me", Map.of("id", 100, "tags", List.of("normal", "test_user")));
    }

    private void givenRejectedSubscriptionWithoutEmail(String correo) {
        givenCurrentSubscriptionPayment("rejected");
        respuestas.put("GET /v1/payments/123", rejectedSubscriptionWithoutEmail(correo));
        Map<String, Object> contrato = contratoJson();
        contrato.put("payer_id", 300);
        respuestas.put("GET /preapproval/mp-contrato", contrato);
    }

    private static Map<String, Object> rejectedSubscriptionWithoutEmail(String correo) {
        Map<String, Object> pago = currentSubscriptionPayment("rejected");
        Map<String, Object> comprador = compradorSinCorreo("300");
        givenNullableField(comprador, "email", correo);
        pago.put("payer", comprador);
        return pago;
    }

    private static Map<String, Object> compradorSinCorreo(String compradorId) {
        Map<String, Object> comprador = new HashMap<>();
        givenNullableField(comprador, "id", compradorId);
        return comprador;
    }

    private static void givenNullableField(Map<String, Object> valores, String campo, String valor) {
        if ("ausente".equals(valor)) {
            valores.remove(campo);
        } else {
            valores.put(campo, "nulo".equals(valor) ? null : valor);
        }
    }

    private static Map<String, Object> currentSubscriptionPayment(String estado) {
        Map<String, Object> pago = pagoJson();
        pago.remove("application_id");
        pago.put("status", estado);
        pago.put("live_mode", true);
        pago.put("payer", Map.of("email", "test_user_123@testuser.com"));
        pago.put("point_of_interaction", Map.of(
                "transaction_data", Map.of("subscription_id", "mp-contrato"),
                "references", List.of(Map.of("type", "PREFERENCE", "id", "preferencia"),
                        Map.of("type", "RECURRING_INVOICE", "id", "factura-recurrente"))));
        return pago;
    }

    private void givenInvalidCurrentSubscriptionPayment(int caso) {
        givenCurrentSubscriptionPayment("approved");
        Map<String, Object> pago = currentSubscriptionPayment("approved");
        Map<String, Object> interaccion = new HashMap<>();
        interaccion.put("transaction_data", Map.of("subscription_id", "mp-contrato"));
        interaccion.put("references", List.of(Map.of("type", "RECURRING_INVOICE", "id", "factura-recurrente")));
        pago.put("point_of_interaction", interaccion);
        switch (caso) {
            case 0 -> respuestas.put("GET /users/me", Map.of("id", 100, "tags", List.of("normal")));
            case 1 -> respuestas.put("GET /users/me", Map.of("id", 999, "tags", List.of("test_user")));
            case 2 -> pago.put("payer", Map.of("email", "persona@example.com"));
            case 3 -> interaccion.put("transaction_data", Map.of("subscription_id", "otro-contrato"));
            case 4 -> interaccion.put("transaction_data", Map.of());
            case 5 -> interaccion.put("references", List.of());
            case 6 -> interaccion.put("references", List.of(
                    Map.of("type", "RECURRING_INVOICE", "id", "una"),
                    Map.of("type", "RECURRING_INVOICE", "id", "otra")));
            case 7 -> interaccion.put("references", List.of(Map.of("type", "RECURRING_INVOICE")));
            case 8 -> interaccion.put("references", List.of(Map.of("type", "PREFERENCE", "id", "preferencia")));
            case 9 -> {
                Map<String, Object> contrato = contratoJson();
                contrato.put("id", "otro-contrato");
                respuestas.put("GET /preapproval/mp-contrato", contrato);
            }
            case 10 -> pago.put("application_id", "otra-aplicacion");
            case 11 -> respuestas.put("GET /users/me", Map.of("id", 100));
            case 12 -> pago.remove("payer");
            default -> throw new IllegalArgumentException();
        }
        respuestas.put("GET /v1/payments/123", pago);
    }

    @Test
    @DisplayName("Sin notificaciones configuradas no envía una URL vacía al proveedor")
    void crearShouldOmitUnconfiguredWebhookUrl() throws Exception {
        properties.setNotificationUrl("");

        adapter.crear(renovacion(), "cliente@example.test");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(cliente).send(captor.capture(), any());
        assertThat(JSON.readTree(contenido(captor.getValue())).has("notification_url")).isFalse();
    }

    private static Map<String, Object> contratoJson() {
        Map<String, Object> contrato = new HashMap<>();
        contrato.put("id", "mp-contrato");
        contrato.put("external_reference", RENOVACION_ID.toString());
        contrato.put("init_point", "https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=mp-contrato");
        contrato.put("status", "pending");
        contrato.put("collector_id", "100");
        contrato.put("application_id", "200");
        contrato.put("auto_recurring", recurrencia());
        contrato.put("next_payment_date", AHORA.plusDays(30).toString());
        contrato.put("last_modified", AHORA.toString());
        return contrato;
    }

    private static Map<String, Object> recurrencia() {
        return Map.of("frequency", 30, "frequency_type", "days", "transaction_amount", "14900.00",
                "currency_id", "ARS", "start_date", AHORA.toString());
    }

    private static Map<String, Object> facturaJson() {
        Map<String, Object> factura = new HashMap<>();
        factura.put("id", 456);
        factura.put("preapproval_id", "mp-contrato");
        factura.put("external_reference", RENOVACION_ID.toString());
        factura.put("transaction_amount", "14900.00");
        factura.put("currency_id", "ARS");
        factura.put("debit_date", AHORA.toString());
        factura.put("payment", Map.of("id", 123, "status", "approved"));
        return factura;
    }

    private static Map<String, Object> pagoJson() {
        Map<String, Object> pago = new HashMap<>();
        pago.put("id", 123);
        pago.put("collector_id", "100");
        pago.put("application_id", "200");
        pago.put("external_reference", RENOVACION_ID.toString());
        pago.put("transaction_amount", 14900);
        pago.put("currency_id", "ARS");
        pago.put("status", "approved");
        pago.put("live_mode", false);
        pago.put("date_last_updated", AHORA.toString());
        return pago;
    }

    static MercadoPagoProperties propiedades() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setHabilitado(true);
        properties.setAccessToken("token-de-prueba");
        properties.setCollectorId("100");
        properties.setApplicationId("200");
        properties.setWebhookSecret(SECRETO);
        properties.setNotificationUrl("https://ensambla.example.test/api/pagos/mercadopago");
        properties.setReturnUrl("https://ensambla.example.test/planes");
        return properties;
    }

    private String firma() throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(("id:123;request-id:solicitud;ts:" + FIRMA_TIEMPO + ";")
                .getBytes(StandardCharsets.UTF_8)));
        return "ts=" + FIRMA_TIEMPO + ",v1=" + hash;
    }

    private String contenido(HttpRequest request) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            public void onNext(ByteBuffer buffer) {
                byte[] contenido = new byte[buffer.remaining()];
                buffer.get(contenido);
                bytes.writeBytes(contenido);
            }
            public void onError(Throwable error) {
                throw new AssertionError(error);
            }
            public void onComplete() {
            }
        });
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
