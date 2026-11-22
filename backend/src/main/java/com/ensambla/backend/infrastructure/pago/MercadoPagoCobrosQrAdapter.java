package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.caja.CobroQrId;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.mercadopago.exceptions.MPInvalidWebhookSignatureException;
import com.mercadopago.webhook.WebhookSignatureValidator;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Orders API para QR híbrido. El SDK oficial valida la firma; la creación usa REST porque el SDK aún no modela config.qr. */
public class MercadoPagoCobrosQrAdapter implements PasarelaCobrosQrPort {

    private static final Map<String, EstadoOrdenCobroQr> ESTADOS = Map.ofEntries(
            Map.entry("created", EstadoOrdenCobroQr.PENDIENTE),
            Map.entry("action_required", EstadoOrdenCobroQr.PENDIENTE),
            Map.entry("processing", EstadoOrdenCobroQr.PENDIENTE),
            Map.entry("processed", EstadoOrdenCobroQr.COBRADO),
            Map.entry("canceled", EstadoOrdenCobroQr.CANCELADO),
            Map.entry("cancelled", EstadoOrdenCobroQr.CANCELADO),
            Map.entry("expired", EstadoOrdenCobroQr.VENCIDO),
            Map.entry("refunded", EstadoOrdenCobroQr.REEMBOLSADO));

    private final MercadoPagoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public MercadoPagoCobrosQrAdapter(MercadoPagoProperties properties, ObjectMapper objectMapper,
                                      HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean disponible() {
        return properties.isHabilitado() && !properties.getQrExternalPosId().isBlank();
    }

    @Override
    public OrdenCobroQr crear(CobroQrId referencia, BigDecimal importe) {
        exigirDisponible();
        return orden(peticion("POST", "/v1/orders", cuerpo(referencia, importe), referencia.toString()));
    }

    @Override
    public OrdenCobroQr consultar(String ordenId) {
        exigirDisponible();
        return orden(peticion("GET", "/v1/orders/" + identificador(ordenId), null, null));
    }

    @Override
    public OrdenCobroQr cancelar(String ordenId) {
        exigirDisponible();
        return orden(peticion("POST", "/v1/orders/" + identificador(ordenId) + "/cancel", null,
                "cancel-" + identificador(ordenId)));
    }

    @Override
    public void validarNotificacion(String firma, String solicitudId, String recursoId) {
        exigirDisponible();
        if (firma == null || solicitudId == null || recursoId == null || properties.getWebhookSecret().isBlank()) {
            throw new NotificacionPagoInvalidaException();
        }
        try {
            WebhookSignatureValidator.validate(firma, solicitudId, recursoId, properties.getWebhookSecret());
        } catch (MPInvalidWebhookSignatureException | IllegalArgumentException exception) {
            throw new NotificacionPagoInvalidaException();
        }
    }

    private Map<String, Object> cuerpo(CobroQrId referencia, BigDecimal importe) {
        String monto = importe.setScale(2).toPlainString();
        Map<String, Object> qr = Map.of("external_pos_id", properties.getQrExternalPosId(), "mode", "hybrid");
        Map<String, Object> pago = Map.of("amount", monto);
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "qr");
        cuerpo.put("total_amount", monto);
        cuerpo.put("description", "Cobro de Caja Ensambla");
        cuerpo.put("external_reference", referencia.toString());
        cuerpo.put("expiration_time", "PT15M");
        cuerpo.put("config", Map.of("qr", qr));
        cuerpo.put("transactions", Map.of("payments", List.of(pago)));
        return cuerpo;
    }

    private OrdenCobroQr orden(JsonNode nodo) {
        exigirTexto(nodo, "type", "qr");
        exigirTexto(nodo, "currency", "ARS");
        exigirTexto(nodo, "user_id", properties.getCollectorId());
        String id = texto(nodo, "id");
        CobroQrId referencia = CobroQrId.de(texto(nodo, "external_reference"));
        BigDecimal importe = decimal(nodo, "total_amount");
        EstadoOrdenCobroQr estado = estado(texto(nodo, "status"));
        JsonNode pago = primerPago(nodo);
        String pagoId = pago.path("id").asString("");
        if (pagoId.isBlank()) {
            throw new PasarelaPagoException();
        }
        BigDecimal importePago = decimal(pago, "amount");
        if (importe.compareTo(importePago) != 0) {
            throw new PasarelaPagoException();
        }
        return new OrdenCobroQr(id, referencia, importe, estado, nodo.path("status_detail").asString(""),
                pagoId, nodo.path("type_response").path("qr_data").asString(null),
                properties.getQrStaticData().isBlank() ? null : properties.getQrStaticData());
    }

    private static JsonNode primerPago(JsonNode nodo) {
        JsonNode pagos = nodo.path("transactions").path("payments");
        if (!pagos.isArray() || pagos.size() != 1) {
            throw new PasarelaPagoException();
        }
        return pagos.get(0);
    }

    private static EstadoOrdenCobroQr estado(String estado) {
        EstadoOrdenCobroQr conocido = ESTADOS.get(estado);
        if (conocido == null) {
            throw new PasarelaPagoException();
        }
        return conocido;
    }

    private void exigirTexto(JsonNode nodo, String campo, String esperado) {
        if (!esperado.equals(texto(nodo, campo))) {
            throw new PasarelaPagoException();
        }
    }

    private static String texto(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asString("");
        if (valor.isBlank()) {
            throw new PasarelaPagoException();
        }
        return valor;
    }

    private static BigDecimal decimal(JsonNode nodo, String campo) {
        try {
            return new BigDecimal(texto(nodo, campo));
        } catch (NumberFormatException exception) {
            throw new PasarelaPagoException();
        }
    }

    private JsonNode peticion(String metodo, String camino, Object cuerpo, String idempotencia) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(properties.getApiUrl() + camino))
                    .timeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .header("Content-Type", "application/json");
            if (idempotencia != null) {
                builder.header("X-Idempotency-Key", idempotencia);
            }
            HttpRequest.BodyPublisher contenido = cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(cuerpo));
            HttpResponse<String> respuesta = httpClient.send(builder.method(metodo, contenido).build(),
                    HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() < 200 || respuesta.statusCode() >= 300) {
                throw new PasarelaPagoException();
            }
            return objectMapper.readTree(respuesta.body());
        } catch (IOException | JacksonException exception) {
            throw new PasarelaPagoException();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PasarelaPagoException();
        }
    }

    private void exigirDisponible() {
        if (!disponible()) {
            throw new PasarelaPagoException();
        }
    }

    private static String identificador(String valor) {
        if (valor == null || !valor.matches("[A-Za-z0-9_-]{1,128}")) {
            throw new PasarelaPagoException();
        }
        return valor;
    }
}
