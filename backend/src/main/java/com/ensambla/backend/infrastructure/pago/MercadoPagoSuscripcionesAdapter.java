package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.common.Identificador;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** No registra cuerpos, tokens ni datos del medio de pago. Nunca sigue redirecciones de la API. */
public class MercadoPagoSuscripcionesAdapter implements PasarelaSuscripcionesPort {

    private static final DateTimeFormatter FECHA_MERCADO_PAGO =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSXXX");

    private final MercadoPagoProperties properties;

    private final ObjectMapper objectMapper;

    private final HttpClient httpClient;

    public MercadoPagoSuscripcionesAdapter(MercadoPagoProperties properties, ObjectMapper objectMapper,
                                           HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public boolean disponible() {
        return properties.isHabilitado();
    }

    @Override
    public ContratoRemoto crear(RenovacionPremium renovacion, String email) {
        Map<String, Object> recurrencia = Map.of(
                "frequency", 30,
                "frequency_type", "days",
                "start_date", FECHA_MERCADO_PAGO.format(renovacion.getPrimerCobroEn()),
                "transaction_amount", renovacion.getImporte(),
                "currency_id", renovacion.getMoneda());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reason", "Ensambla Premium - cada 30 días");
        body.put("external_reference", renovacion.getId().toString());
        body.put("payer_email", email);
        body.put("auto_recurring", recurrencia);
        body.put("back_url", properties.getReturnUrl());
        if (!properties.getNotificationUrl().isBlank()) {
            body.put("notification_url", properties.getNotificationUrl());
        }
        body.put("status", "pending");
        return contrato(peticion("POST", "/preapproval", body));
    }

    @Override
    public Optional<ContratoRemoto> buscar(RenovacionId referencia) {
        JsonNode resultados = peticion("GET", "/preapproval/search?q=" + referencia + "&limit=2", null)
                .path("results");
        if (resultados.size() == 0) {
            return Optional.empty();
        }
        if (resultados.size() != 1) {
            throw new PasarelaPagoException();
        }
        return Optional.of(consultar(texto(resultados.get(0), "id")));
    }

    @Override
    public ContratoRemoto consultar(String proveedorId) {
        return contrato(peticion("GET", "/preapproval/" + identificador(proveedorId), null));
    }

    @Override
    public void cancelar(String proveedorId) {
        peticion("PUT", "/preapproval/" + identificador(proveedorId), Map.of("status", "cancelled"));
    }

    @Override
    public List<PagoRemoto> listarPagos(String proveedorId) {
        List<PagoRemoto> pagos = new ArrayList<>();
        int offset = 0;
        int total;
        do {
            JsonNode respuesta = peticion("GET", "/authorized_payments/search?preapproval_id="
                    + identificador(proveedorId) + "&offset=" + offset, null);
            JsonNode resultados = respuesta.path("results");
            total = respuesta.path("paging").path("total").asInt(-1);
            if (!resultados.isArray() || total < 0 || (resultados.isEmpty() && offset < total)) {
                throw new PasarelaPagoException();
            }
            for (JsonNode factura : resultados) {
                if (!factura.path("payment").path("id").isMissingNode()
                        && !factura.path("payment").path("id").isNull()) {
                    pagos.add(pago(factura, proveedorId));
                }
            }
            offset += resultados.size();
        } while (offset < total);
        return pagos;
    }

    @Override
    public String identificarContrato(String tipo, String recursoId) {
        return switch (tipo) {
            case "subscription_preapproval" -> identificador(recursoId);
            case "subscription_authorized_payment" -> texto(peticion("GET", "/authorized_payments/"
                    + identificador(recursoId), null), "preapproval_id");
            case "payment" -> contratoDelPago(recursoId);
            default -> "";
        };
    }

    @Override
    public void validarNotificacion(String firma, String solicitudId, String recursoId) {
        exigirDisponible();
        if (firma == null || solicitudId == null || recursoId == null || properties.getWebhookSecret().isBlank()) {
            throw new NotificacionPagoInvalidaException();
        }
        Map<String, String> partes = new LinkedHashMap<>();
        for (String parte : firma.split(",")) {
            String[] par = parte.trim().split("=", 2);
            if (par.length == 2) {
                partes.put(par[0], par[1]);
            }
        }
        String tiempo = partes.getOrDefault("ts", "");
        String recibida = partes.getOrDefault("v1", "");
        if (!tiempo.matches("[0-9]{10,13}") || !recibida.matches("[0-9a-f]{64}")) {
            throw new NotificacionPagoInvalidaException();
        }
        String manifiesto = "id:" + recursoId + ";request-id:" + solicitudId + ";ts:" + tiempo + ";";
        String calculada = hmac(manifiesto);
        if (!MessageDigest.isEqual(calculada.getBytes(StandardCharsets.UTF_8),
                recibida.getBytes(StandardCharsets.UTF_8))) {
            throw new NotificacionPagoInvalidaException();
        }
    }

    private String contratoDelPago(String recursoId) {
        JsonNode resultados = peticion("GET", "/authorized_payments/search?payment_id="
                + identificador(recursoId), null).path("results");
        return resultados.size() == 1 ? texto(resultados.get(0), "preapproval_id") : "";
    }

    private ContratoRemoto contrato(JsonNode nodo) {
        validarCuenta(nodo);
        JsonNode recurrencia = nodo.path("auto_recurring");
        String enlace = nodo.path("init_point").asString("");
        if (!enlace.isEmpty() && !enlace.startsWith("https://www.mercadopago.com.ar/subscriptions/checkout?")) {
            throw new PasarelaPagoException();
        }
        return new ContratoRemoto(texto(nodo, "id"), referencia(nodo), enlace, texto(nodo, "status"),
                importe(recurrencia), texto(recurrencia, "currency_id"),
                recurrencia.path("frequency").asInt(), texto(recurrencia, "frequency_type"),
                fecha(recurrencia, "start_date"), fechaOpcional(nodo, "next_payment_date"),
                fecha(nodo, "last_modified"));
    }

    private PagoRemoto pago(JsonNode factura, String proveedorId) {
        String pagoId = texto(factura.path("payment"), "id");
        JsonNode pago = peticion("GET", "/v1/payments/" + identificador(pagoId), null);
        validarCuentaDelPago(pago, proveedorId);
        JsonNode datosContrato = peticion("GET", "/preapproval/" + identificador(proveedorId), null);
        ContratoRemoto contrato = contrato(datosContrato);
        if (!proveedorId.equals(contrato.id())
                || !referencia(factura).equals(contrato.referencia())
                || !proveedorId.equals(texto(factura, "preapproval_id"))
                || !referencia(factura).equals(referencia(pago))
                || importe(factura).compareTo(importe(pago)) != 0
                || !texto(factura, "currency_id").equals(texto(pago, "currency_id"))
                || !pagoId.equals(texto(pago, "id"))) {
            throw new PasarelaPagoException();
        }
        validarAmbiente(pago, datosContrato);
        return new PagoRemoto(identificarFactura(factura, pago), pagoId, proveedorId, referencia(factura),
                importe(pago), texto(pago, "currency_id"),
                texto(pago, "status"), fecha(factura, "debit_date"), fecha(pago, "date_last_updated"));
    }

    private void validarCuentaDelPago(JsonNode pago, String proveedorId) {
        if (!properties.getCollectorId().equals(texto(pago, "collector_id"))) {
            throw new PasarelaPagoException();
        }
        String aplicacion = pago.path("application_id").asString("");
        String suscripcion = pago.path("point_of_interaction").path("transaction_data")
                .path("subscription_id").asString("");
        if (!suscripcion.isBlank() && !proveedorId.equals(suscripcion)) {
            throw new PasarelaPagoException();
        }
        if (aplicacion.isBlank()) {
            // El formato nuevo identifica la aplicación en el contrato y enlaza el pago a la suscripción.
            if (suscripcion.isBlank()) {
                throw new PasarelaPagoException();
            }
        } else if (!properties.getApplicationId().equals(aplicacion)) {
            throw new PasarelaPagoException();
        }
    }

    private void validarAmbiente(JsonNode pago, JsonNode contrato) {
        JsonNode ambiente = pago.path("live_mode");
        if (!ambiente.isBoolean()) {
            throw new PasarelaPagoException();
        }
        if (properties.isProduccion()) {
            if (!ambiente.asBoolean()) {
                throw new PasarelaPagoException();
            }
            return;
        }
        if (!ambiente.asBoolean()) {
            return;
        }
        // Suscripciones puede devolver live_mode=true también con cuentas y tarjetas de prueba.
        JsonNode vendedor = peticion("GET", "/users/me", null);
        if (!properties.getCollectorId().equals(texto(vendedor, "id"))
                || !esCuentaDePrueba(vendedor)) {
            throw new PasarelaPagoException();
        }
        if (!pago.path("payer").path("email").asString("").endsWith("@testuser.com")
                && !esRechazoSinCorreoVinculado(pago, contrato)) {
            throw new PasarelaPagoException();
        }
    }

    private boolean esRechazoSinCorreoVinculado(JsonNode pago, JsonNode contrato) {
        // La omisión de correo sólo permite registrar un rechazo; nunca habilita un pago aprobado.
        if (!"rejected".equals(pago.path("status").asString(""))
                || !pago.path("payer").path("email").asString("").isBlank()) {
            return false;
        }
        return texto(pago.path("payer"), "id").equals(texto(contrato, "payer_id"));
    }

    private boolean esCuentaDePrueba(JsonNode cuenta) {
        for (JsonNode etiqueta : cuenta.path("tags")) {
            if ("test_user".equals(etiqueta.asString(""))) {
                return true;
            }
        }
        return false;
    }

    private String identificarFactura(JsonNode factura, JsonNode pago) {
        List<String> referencias = new ArrayList<>();
        for (JsonNode referencia : pago.path("point_of_interaction").path("references")) {
            if ("RECURRING_INVOICE".equals(referencia.path("type").asString(""))) {
                referencias.add(texto(referencia, "id"));
            }
        }
        if (referencias.size() > 1) {
            throw new PasarelaPagoException();
        }
        if (!referencias.isEmpty()) {
            // El id público numérico y la referencia interna no comparten espacio de nombres.
            return referencias.getFirst();
        }
        return texto(factura, "id");
    }

    private void validarCuenta(JsonNode nodo) {
        if (!properties.getCollectorId().equals(texto(nodo, "collector_id"))
                || !properties.getApplicationId().equals(texto(nodo, "application_id"))) {
            throw new PasarelaPagoException();
        }
    }

    private JsonNode peticion(String metodo, String ruta, Object body) {
        exigirDisponible();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.getApiUrl() + ruta))
                    .timeout(Duration.ofSeconds(5))
                    .header("Authorization", "Bearer " + properties.getAccessToken())
                    .header("Content-Type", "application/json")
                    .method(metodo, body == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PasarelaPagoException();
            }
            return objectMapper.readTree(response.body());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PasarelaPagoException();
        } catch (IOException | RuntimeException exception) {
            throw new PasarelaPagoException();
        }
    }

    private String identificador(String valor) {
        if (!valor.matches("[a-zA-Z0-9_-]{1,100}")) {
            throw new BusinessRuleException("Identificador de pago inválido.");
        }
        return valor;
    }

    private String texto(JsonNode nodo, String campo) {
        String valor = nodo.path(campo).asString("");
        if (valor.isBlank()) {
            throw new PasarelaPagoException();
        }
        return valor;
    }

    private RenovacionId referencia(JsonNode nodo) {
        try {
            return RenovacionId.de(texto(nodo, "external_reference"));
        } catch (IllegalArgumentException exception) {
            throw new PasarelaPagoException();
        }
    }

    private BigDecimal importe(JsonNode nodo) {
        try {
            return new BigDecimal(texto(nodo, "transaction_amount"));
        } catch (NumberFormatException exception) {
            throw new PasarelaPagoException();
        }
    }

    private OffsetDateTime fecha(JsonNode nodo, String campo) {
        try {
            return OffsetDateTime.parse(texto(nodo, campo));
        } catch (DateTimeException exception) {
            throw new PasarelaPagoException();
        }
    }

    private OffsetDateTime fechaOpcional(JsonNode nodo, String campo) {
        return nodo.path(campo).isMissingNode() || nodo.path(campo).isNull() ? null : fecha(nodo, campo);
    }

    private String hmac(String manifiesto) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("No se pudo validar la firma de pago", exception);
        }
    }
}
