package com.ensambla.backend.support;

import com.ensambla.backend.infrastructure.pago.MercadoPagoProperties;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La API de Mercado Pago simulada para probar el adaptador: cada petición ({@code "GET /ruta?consulta"}) devuelve la
 * respuesta que se le cargó y, si no tiene una, el contrato de prueba. También trae los JSON de contrato, factura y
 * pago que devuelve la API real, y la configuración y la firma de las notificaciones.
 */
public final class MercadoPagoDePrueba {

    public static final ObjectMapper JSON = new ObjectMapper();
    public static final String SECRETO = "secreto-exclusivamente-de-prueba";
    public static final String FIRMA_TIEMPO = "1792497600";
    public static final String CONTRATO = "mp-contrato";
    public static final String VENDEDOR = "100";
    public static final String COMPRADOR_DEL_CONTRATO = "300";
    public static final String ORDER = "ORD01ENSAMBLA";

    private final HttpClient cliente = mock(HttpClient.class);
    private final HttpResponse<String> respuestaPorDefecto = respuestaMock();
    private final Map<String, Object> respuestas = new HashMap<>();

    public MercadoPagoDePrueba() {
        try {
            when(respuestaPorDefecto.statusCode()).thenReturn(200);
            when(respuestaPorDefecto.body()).thenAnswer(invocacion -> JSON.writeValueAsString(contrato()));
            when(cliente.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                    .thenAnswer(invocacion -> responder(invocacion.getArgument(0)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public HttpClient cliente() {
        return cliente;
    }

    // --- lo que responde la API ---

    public void responderA(String peticion, Object cuerpo) {
        respuestas.put(peticion, cuerpo);
    }

    public void responderSiempreConEstado(int estado) {
        when(respuestaPorDefecto.statusCode()).thenReturn(estado);
    }

    public void responderSiempreCon(String cuerpoCrudo) {
        when(respuestaPorDefecto.body()).thenReturn(cuerpoCrudo);
    }

    public void fallarCon(Exception error) {
        try {
            doThrow(error).when(cliente).send(any(), any());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void responderConLasFacturas(List<Map<String, Object>> facturas) {
        responderA("GET /authorized_payments/search?preapproval_id=" + CONTRATO + "&offset=0",
                Map.of("paging", Map.of("total", facturas.size()), "results", facturas));
    }

    public void responderConElPago(Map<String, Object> pago) {
        responderA("GET /v1/payments/123", pago);
    }

    public void responderConElContrato(Map<String, Object> contrato) {
        responderA("GET /preapproval/" + CONTRATO, contrato);
    }

    public void responderConElVendedor(Map<String, Object> vendedor) {
        responderA("GET /users/me", vendedor);
    }

    /** Un pago con el formato que hoy devuelve Suscripciones, con un vendedor de prueba verificado. */
    public void responderConUnPagoDeSuscripcion(String estado) {
        Map<String, Object> factura = factura();
        factura.remove("id");
        responderConLasFacturas(List.of(factura));
        responderConElPago(pagoDeSuscripcion(estado));
        responderConElVendedor(Map.of("id", 100, "tags", List.of("normal", "test_user")));
    }

    /** Un rechazo de prueba cuyo comprador no tiene el correo {@code correo} y coincide con el del contrato. */
    public void responderConUnRechazoSinCorreo(String correo) {
        responderConUnPagoDeSuscripcion("rejected");
        responderConElPago(rechazoSinCorreo(correo));
        Map<String, Object> contrato = contrato();
        contrato.put("payer_id", 300);
        responderConElContrato(contrato);
    }

    // --- lo que se le envió ---

    public HttpRequest unicaPeticionEnviada() {
        try {
            var captor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
            verify(cliente).send(captor.capture(), any());
            return captor.getValue();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static JsonNode cuerpoDe(HttpRequest request) {
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
        return JSON.readTree(bytes.toString(StandardCharsets.UTF_8));
    }

    // --- los JSON de la API ---

    public static Map<String, Object> contrato() {
        Map<String, Object> contrato = new HashMap<>();
        contrato.put("id", CONTRATO);
        contrato.put("external_reference", RENOVACION_ID.toString());
        contrato.put("init_point", "https://www.mercadopago.com.ar/subscriptions/checkout?preapproval_id=mp-contrato");
        contrato.put("status", "pending");
        contrato.put("collector_id", VENDEDOR);
        contrato.put("application_id", "200");
        contrato.put("auto_recurring", recurrencia());
        contrato.put("next_payment_date", AHORA.plusDays(30).toString());
        contrato.put("last_modified", AHORA.toString());
        return contrato;
    }

    public static Map<String, Object> recurrencia() {
        return Map.of("frequency", 30, "frequency_type", "days", "transaction_amount", "14900.00",
                "currency_id", "ARS", "start_date", AHORA.toString());
    }

    public static Map<String, Object> factura() {
        Map<String, Object> factura = new HashMap<>();
        factura.put("id", 456);
        factura.put("preapproval_id", CONTRATO);
        factura.put("external_reference", RENOVACION_ID.toString());
        factura.put("transaction_amount", "14900.00");
        factura.put("currency_id", "ARS");
        factura.put("debit_date", AHORA.toString());
        factura.put("payment", Map.of("id", 123, "status", "approved"));
        return factura;
    }

    public static Map<String, Object> pago() {
        Map<String, Object> pago = new HashMap<>();
        pago.put("id", 123);
        pago.put("collector_id", VENDEDOR);
        pago.put("application_id", "200");
        pago.put("external_reference", RENOVACION_ID.toString());
        pago.put("transaction_amount", 14900);
        pago.put("currency_id", "ARS");
        pago.put("status", "approved");
        pago.put("live_mode", false);
        pago.put("date_last_updated", AHORA.toString());
        return pago;
    }

    public static Map<String, Object> order(String estado) {
        Map<String, Object> order = new HashMap<>();
        order.put("id", ORDER);
        order.put("type", "qr");
        order.put("external_reference", com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID.toString());
        order.put("total_amount", "2500.50");
        order.put("currency", "ARS");
        order.put("user_id", VENDEDOR);
        order.put("status", estado);
        order.put("status_detail", estado + "_detail");
        order.put("transactions", Map.of("payments", List.of(Map.of("id", "PAY01", "amount", "2500.50"))));
        order.put("type_response", Map.of("qr_data", "qr-dinamico"));
        return order;
    }

    public static Map<String, Object> pagoDeSuscripcion(String estado) {
        Map<String, Object> pago = pago();
        pago.remove("application_id");
        pago.put("status", estado);
        pago.put("live_mode", true);
        pago.put("payer", Map.of("email", "test_user_123@testuser.com"));
        pago.put("point_of_interaction", Map.of(
                "transaction_data", Map.of("subscription_id", CONTRATO),
                "references", List.of(Map.of("type", "PREFERENCE", "id", "preferencia"),
                        Map.of("type", "RECURRING_INVOICE", "id", "factura-recurrente"))));
        return pago;
    }

    /** Un pago de suscripción con la interacción editable, para alterarle la suscripción o las referencias. */
    public static Map<String, Object> pagoDeSuscripcionConInteraccion(Map<String, Object> interaccion) {
        Map<String, Object> pago = pagoDeSuscripcion("approved");
        pago.put("point_of_interaction", interaccion);
        return pago;
    }

    public static Map<String, Object> interaccionDeSuscripcion() {
        Map<String, Object> interaccion = new HashMap<>();
        interaccion.put("transaction_data", Map.of("subscription_id", CONTRATO));
        interaccion.put("references", List.of(Map.of("type", "RECURRING_INVOICE", "id", "factura-recurrente")));
        return interaccion;
    }

    public static Map<String, Object> rechazoSinCorreo(String correo) {
        Map<String, Object> pago = pagoDeSuscripcion("rejected");
        Map<String, Object> comprador = compradorSinCorreo(COMPRADOR_DEL_CONTRATO);
        conCampo(comprador, "email", correo);
        pago.put("payer", comprador);
        return pago;
    }

    public static Map<String, Object> compradorSinCorreo(String compradorId) {
        Map<String, Object> comprador = new HashMap<>();
        conCampo(comprador, "id", compradorId);
        return comprador;
    }

    /** {@code "ausente"} saca el campo, {@code "nulo"} lo deja en {@code null} y cualquier otro valor lo pone. */
    public static void conCampo(Map<String, Object> valores, String campo, String valor) {
        Map<String, Runnable> segunElValor = Map.of(
                "ausente", () -> valores.remove(campo),
                "nulo", () -> valores.put(campo, null));
        segunElValor.getOrDefault(valor, () -> valores.put(campo, valor)).run();
    }

    // --- configuración y firma ---

    public static MercadoPagoProperties propiedades() {
        MercadoPagoProperties properties = new MercadoPagoProperties();
        properties.setHabilitado(true);
        properties.setAccessToken("token-de-prueba");
        properties.setCollectorId(VENDEDOR);
        properties.setApplicationId("200");
        properties.setWebhookSecret(SECRETO);
        properties.setNotificationUrl("https://ensambla.example.test/api/pagos/mercadopago");
        properties.setReturnUrl("https://ensambla.example.test/planes");
        properties.setQrExternalPosId("ENSAMBLA_POS_01");
        properties.setQrStaticData("qr-estatico");
        return properties;
    }

    /** La firma que manda Mercado Pago para la notificación del recurso 123 con la solicitud {@code solicitud}. */
    public static String firmaValida() {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String hash = HexFormat.of().formatHex(mac.doFinal(("id:123;request-id:solicitud;ts:" + FIRMA_TIEMPO + ";")
                    .getBytes(StandardCharsets.UTF_8)));
            return "ts=" + FIRMA_TIEMPO + ",v1=" + hash;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private HttpResponse<String> responder(HttpRequest request) {
        Object cuerpo = respuestas.get(request.method() + " " + request.uri().getRawPath()
                + (request.uri().getRawQuery() == null ? "" : "?" + request.uri().getRawQuery()));
        if (cuerpo == null) {
            return respuestaPorDefecto;
        }
        HttpResponse<String> individual = respuestaMock();
        when(individual.statusCode()).thenReturn(200);
        when(individual.body()).thenReturn(JSON.writeValueAsString(cuerpo));
        return individual;
    }

    @SuppressWarnings("unchecked")
    private static HttpResponse<String> respuestaMock() {
        return mock(HttpResponse.class);
    }
}
