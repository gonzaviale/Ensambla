package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.model.caja.OrdenCobroQr;
import com.ensambla.backend.support.MercadoPagoDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.MercadoPagoDePrueba.JSON;
import static com.ensambla.backend.support.MercadoPagoDePrueba.ORDER;
import static com.ensambla.backend.support.MercadoPagoDePrueba.SECRETO;
import static com.ensambla.backend.support.MercadoPagoDePrueba.order;
import static com.ensambla.backend.support.MercadoPagoDePrueba.propiedades;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MercadoPagoCobrosQrAdapterTest {

    private MercadoPagoDePrueba api;
    private MercadoPagoProperties properties;
    private MercadoPagoCobrosQrAdapter adapter;
    private OrdenCobroQr respuesta;
    private Throwable rechazo;

    @BeforeEach
    void dadoElAdaptadorDeMercadoPago() {
        api = new MercadoPagoDePrueba();
        properties = propiedades();
        adapter = new MercadoPagoCobrosQrAdapter(properties, JSON, api.cliente());
    }

    @Test
    @DisplayName("Crea una order QR híbrida con importe, caja e idempotencia")
    void creaUnaOrderQrHibridaConImporteCajaEIdempotencia() {
        dadoQueMercadoPagoDevuelve(order("created"));
        cuandoSeCreaLaOrder();
        entoncesSeCreaLaOrderHibrida();
    }

    @ParameterizedTest
    @CsvSource({"created,PENDIENTE", "action_required,PENDIENTE", "processing,PENDIENTE",
            "processed,COBRADO", "canceled,CANCELADO", "cancelled,CANCELADO",
            "expired,VENCIDO", "refunded,REEMBOLSADO"})
    @DisplayName("Consulta todos los estados conocidos de una order")
    void consultaTodosLosEstadosConocidosDeUnaOrder(String remoto, EstadoOrdenCobroQr esperado) {
        dadoQueMercadoPagoDevuelveEn("GET /v1/orders/" + ORDER, order(remoto));
        cuandoSeConsultaLaOrder();
        entoncesLaOrderQuedaEn(esperado);
    }

    @Test
    @DisplayName("Cancela la order usando una clave de idempotencia estable")
    void cancelaLaOrderUsandoUnaClaveDeIdempotenciaEstable() {
        dadoQueMercadoPagoDevuelveEn("POST /v1/orders/" + ORDER + "/cancel", order("canceled"));
        cuandoSeCancelaLaOrder();
        entoncesSeCancelaConIdempotencia();
    }

    @Test
    @DisplayName("Sin caja QR configurada la pasarela no está disponible")
    void sinCajaQrConfiguradaLaPasarelaNoEstaDisponible() {
        dadoSinCajaQrConfigurada();
        cuandoSeConsultaDisponibilidad();
        entoncesNoEstaDisponible();
    }

    @ParameterizedTest
    @ValueSource(strings = {"type", "currency", "user_id"})
    @DisplayName("Rechaza una order que no corresponde al cobro argentino de la cuenta")
    void rechazaUnaOrderQueNoCorrespondeAlCobroArgentinoDeLaCuenta(String campo) {
        dadoQueMercadoPagoDevuelve(orderCon(campo, "otro"));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @ValueSource(strings = {"unknown", ""})
    @DisplayName("Rechaza un estado de order desconocido o vacío")
    void rechazaUnEstadoDeOrderDesconocidoOVacio(String estado) {
        dadoQueMercadoPagoDevuelve(orderCon("status", estado));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order sin una única transacción de pago")
    void rechazaUnaOrderSinUnaUnicaTransaccionDePago() {
        dadoQueMercadoPagoDevuelve(orderCon("transactions", Map.of("payments", List.of())));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order con más de una transacción de pago")
    void rechazaUnaOrderConMasDeUnaTransaccionDePago() {
        dadoQueMercadoPagoDevuelve(orderCon("transactions", Map.of("payments", List.of(
                Map.of("id", "1", "amount", "2500.50"), Map.of("id", "2", "amount", "2500.50")))));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order cuyas transacciones de pago no son una lista")
    void rechazaUnaOrderCuyasTransaccionesDePagoNoSonUnaLista() {
        dadoQueMercadoPagoDevuelve(orderCon("transactions", Map.of("payments", Map.of())));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order cuyo pago no tiene id")
    void rechazaUnaOrderCuyoPagoNoTieneId() {
        dadoQueMercadoPagoDevuelve(orderCon("transactions", Map.of("payments",
                List.of(Map.of("amount", "2500.50")))));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order cuyo pago tiene otro importe")
    void rechazaUnaOrderCuyoPagoTieneOtroImporte() {
        dadoQueMercadoPagoDevuelve(orderCon("transactions", Map.of("payments",
                List.of(Map.of("id", "PAY01", "amount", "1.00")))));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una order con importe ilegible")
    void rechazaUnaOrderConImporteIlegible() {
        dadoQueMercadoPagoDevuelve(orderCon("total_amount", "no-es-un-importe"));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Puede crear la order sin una trama QR fija configurada")
    void puedeCrearLaOrderSinUnaTramaQrFijaConfigurada() {
        properties.setQrStaticData("");
        dadoQueMercadoPagoDevuelve(order("created"));

        cuandoSeCreaLaOrder();

        assertThat(respuesta.qrEstatico()).isNull();
    }

    @ParameterizedTest
    @ValueSource(ints = {199, 300})
    @DisplayName("Rechaza respuestas HTTP que no son exitosas")
    void rechazaRespuestasHttpQueNoSonExitosas(int estado) {
        dadoQueMercadoPagoResponde(estado);
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza una respuesta que no contiene JSON válido")
    void rechazaUnaRespuestaQueNoContieneJsonValido() {
        dadoQueMercadoPagoDevuelveJsonInvalido();
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Rechaza un fallo de red al crear la order")
    void rechazaUnFalloDeRedAlCrearLaOrder() {
        dadoQueMercadoPagoFalla(new IOException("sin red"));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Conserva la interrupción del hilo si la llamada fue interrumpida")
    void conservaLaInterrupcionDelHiloSiLaLlamadaFueInterrumpida() {
        dadoQueMercadoPagoFalla(new InterruptedException("interrumpida"));
        cuandoSeIntentaCrearLaOrder();
        entoncesFallaEInterrumpeElHilo();
    }

    @Test
    @DisplayName("Valida una notificación firmada con el SDK oficial")
    void validaUnaNotificacionFirmadaConElSdkOficial() {
        dadoUnaFirmaValida();
        cuandoSeValidaLaNotificacion();
        entoncesLaFirmaEsAceptada();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {""})
    @DisplayName("Rechaza una notificación sin firma")
    void rechazaUnaNotificacionSinFirma(String firma) {
        cuandoSeIntentaValidar(firma, "solicitud", ORDER);
        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Rechaza una notificación sin id de solicitud")
    void rechazaUnaNotificacionSinIdDeSolicitud() {
        cuandoSeIntentaValidar("firma", null, ORDER);
        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Rechaza una notificación sin id de order")
    void rechazaUnaNotificacionSinIdDeOrder() {
        cuandoSeIntentaValidar("firma", "solicitud", null);
        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Rechaza una notificación si falta el secreto")
    void rechazaUnaNotificacionSiFaltaElSecreto() {
        properties.setWebhookSecret("");
        cuandoSeIntentaValidar("firma", "solicitud", ORDER);
        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Rechaza una firma que no coincide")
    void rechazaUnaFirmaQueNoCoincide() {
        cuandoSeIntentaValidar("ts=1,v1=00", "solicitud", ORDER);
        entoncesLaNotificacionEsInvalida();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"con espacios", "id/con/barra", ""})
    @DisplayName("Rechaza un id de order inseguro antes de llamar a la API")
    void rechazaUnIdDeOrderInseguroAntesDeLlamarALaApi(String id) {
        cuandoSeIntentaConsultar(id);
        entoncesFallaLaPasarela();
    }

    private void dadoQueMercadoPagoDevuelve(Object cuerpo) {
        api.responderA("POST /v1/orders", cuerpo);
    }

    private void dadoQueMercadoPagoDevuelveEn(String peticion, Object cuerpo) {
        api.responderA(peticion, cuerpo);
    }

    private void dadoSinCajaQrConfigurada() {
        properties.setQrExternalPosId("");
    }

    private void dadoQueMercadoPagoResponde(int estado) {
        api.responderSiempreConEstado(estado);
    }

    private void dadoQueMercadoPagoDevuelveJsonInvalido() {
        api.responderSiempreCon("no-es-json");
    }

    private void dadoQueMercadoPagoFalla(Exception error) {
        api.fallarCon(error);
    }

    private String firma;

    private void dadoUnaFirmaValida() {
        firma = firmaPara(ORDER, "solicitud");
    }

    private void cuandoSeCreaLaOrder() {
        respuesta = adapter.crear(COBRO_QR_ID, IMPORTE);
    }

    private void cuandoSeIntentaCrearLaOrder() {
        rechazo = catchThrowable(this::cuandoSeCreaLaOrder);
    }

    private void cuandoSeConsultaLaOrder() {
        respuesta = adapter.consultar(ORDER);
    }

    private void cuandoSeCancelaLaOrder() {
        respuesta = adapter.cancelar(ORDER);
    }

    private void cuandoSeConsultaDisponibilidad() {
        respuesta = null;
    }

    private void cuandoSeValidaLaNotificacion() {
        adapter.validarNotificacion(firma, "solicitud", ORDER);
    }

    private void cuandoSeIntentaValidar(String firmaRecibida, String solicitud, String recurso) {
        rechazo = catchThrowable(() -> adapter.validarNotificacion(firmaRecibida, solicitud, recurso));
    }

    private void cuandoSeIntentaConsultar(String id) {
        rechazo = catchThrowable(() -> adapter.consultar(id));
    }

    private void entoncesSeCreaLaOrderHibrida() {
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenCobroQr.PENDIENTE);
        assertThat(respuesta.qrDinamico()).isEqualTo("qr-dinamico");
        assertThat(respuesta.qrEstatico()).isEqualTo("qr-estatico");
        HttpRequest request = api.unicaPeticionEnviada();
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer token-de-prueba");
        assertThat(request.headers().firstValue("X-Idempotency-Key")).contains(COBRO_QR_ID.toString());
        var cuerpo = MercadoPagoDePrueba.cuerpoDe(request);
        assertThat(cuerpo.path("config").path("qr").path("mode").asString()).isEqualTo("hybrid");
        assertThat(cuerpo.path("config").path("qr").path("external_pos_id").asString())
                .isEqualTo("ENSAMBLA_POS_01");
        assertThat(cuerpo.path("transactions").path("payments").get(0).path("amount").asString())
                .isEqualTo("2500.50");
    }

    private void entoncesLaOrderQuedaEn(EstadoOrdenCobroQr estado) {
        assertThat(respuesta.estado()).isEqualTo(estado);
    }

    private void entoncesSeCancelaConIdempotencia() {
        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenCobroQr.CANCELADO);
        assertThat(api.unicaPeticionEnviada().headers().firstValue("X-Idempotency-Key"))
                .contains("cancel-" + ORDER);
    }

    private void entoncesNoEstaDisponible() {
        assertThat(adapter.disponible()).isFalse();
        rechazo = catchThrowable(this::cuandoSeCreaLaOrder);
        entoncesFallaLaPasarela();
    }

    private void entoncesFallaLaPasarela() {
        assertThat(rechazo).isInstanceOf(PasarelaPagoException.class);
    }

    private void entoncesFallaEInterrumpeElHilo() {
        entoncesFallaLaPasarela();
        assertThat(Thread.interrupted()).isTrue();
    }

    private void entoncesLaFirmaEsAceptada() {
        assertThat(rechazo).isNull();
    }

    private void entoncesLaNotificacionEsInvalida() {
        assertThat(rechazo).isInstanceOf(NotificacionPagoInvalidaException.class);
    }

    private static Map<String, Object> orderCon(String campo, Object valor) {
        Map<String, Object> resultado = new HashMap<>(order("created"));
        resultado.put(campo, valor);
        return resultado;
    }

    private static String firmaPara(String recurso, String solicitud) {
        try {
            String tiempo = String.valueOf(Instant.now().getEpochSecond());
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRETO.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String manifiesto = "id:" + recurso + ";request-id:" + solicitud + ";ts:" + tiempo + ";";
            String hash = HexFormat.of().formatHex(mac.doFinal(manifiesto.getBytes(StandardCharsets.UTF_8)));
            return "ts=" + tiempo + ",v1=" + hash;
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
