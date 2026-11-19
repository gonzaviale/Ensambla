package com.ensambla.backend.infrastructure.pago;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.NotificacionPagoInvalidaException;
import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.support.MercadoPagoDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Mac;

import static com.ensambla.backend.support.MercadoPagoDePrueba.CONTRATO;
import static com.ensambla.backend.support.MercadoPagoDePrueba.FIRMA_TIEMPO;
import static com.ensambla.backend.support.MercadoPagoDePrueba.JSON;
import static com.ensambla.backend.support.MercadoPagoDePrueba.compradorSinCorreo;
import static com.ensambla.backend.support.MercadoPagoDePrueba.conCampo;
import static com.ensambla.backend.support.MercadoPagoDePrueba.contrato;
import static com.ensambla.backend.support.MercadoPagoDePrueba.cuerpoDe;
import static com.ensambla.backend.support.MercadoPagoDePrueba.factura;
import static com.ensambla.backend.support.MercadoPagoDePrueba.firmaValida;
import static com.ensambla.backend.support.MercadoPagoDePrueba.interaccionDeSuscripcion;
import static com.ensambla.backend.support.MercadoPagoDePrueba.pago;
import static com.ensambla.backend.support.MercadoPagoDePrueba.pagoDeSuscripcion;
import static com.ensambla.backend.support.MercadoPagoDePrueba.pagoDeSuscripcionConInteraccion;
import static com.ensambla.backend.support.MercadoPagoDePrueba.rechazoSinCorreo;
import static com.ensambla.backend.support.MercadoPagoDePrueba.recurrencia;
import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mockStatic;

class MercadoPagoSuscripcionesAdapterTest {

    private static final String SOLICITUD = "solicitud";
    private static final String RECURSO = "123";
    private static final String OTRA_REFERENCIA = "00000000-0000-0000-0000-000000000000";

    private MercadoPagoProperties properties;
    private MercadoPagoDePrueba mercadoPago;
    private MercadoPagoSuscripcionesAdapter adapter;
    private ContratoRemoto contrato;
    private List<PagoRemoto> pagos;
    private Throwable error;

    /** Lo usan otros tests del paquete para armar el adaptador con la misma configuración. */
    static MercadoPagoProperties propiedades() {
        return MercadoPagoDePrueba.propiedades();
    }

    @BeforeEach
    void prepararLaApiSimulada() {
        properties = MercadoPagoDePrueba.propiedades();
        mercadoPago = new MercadoPagoDePrueba();
        adapter = new MercadoPagoSuscripcionesAdapter(properties, JSON, mercadoPago.cliente());
    }

    // --- crear ---

    @Test
    @DisplayName("Crea el checkout en la API con el token y un tiempo máximo de espera")
    void creaElCheckoutEnLaApiConElTokenYUnTiempoMaximo() {
        cuandoSeCreaElContrato();

        entoncesSeLlamoA("https://api.mercadopago.com/preapproval");
        entoncesSeAutenticoConElTokenYEsperoHasta(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("Crea un checkout pendiente con precio del servidor y fecha exacta, sin enviar una tarjeta")
    void creaUnCheckoutPendienteConLasCondicionesDelServidor() {
        cuandoSeCreaElContrato();

        entoncesSePidioUnContratoPendientePara("cliente@example.test");
        entoncesSePidioCobrarCadaTreintaDias("14900", "ARS", "2026-10-20T12:00:00.000Z");
        entoncesNoSeMandoUnaTarjeta();
    }

    @Test
    @DisplayName("Devuelve el contrato creado con su referencia, importe y fechas")
    void devuelveElContratoCreado() {
        cuandoSeCreaElContrato();

        entoncesElContratoEsDeLaRenovacionPor("14900");
        entoncesElContratoEmpiezaAhoraYCobraEnTreintaDias();
    }

    @Test
    @DisplayName("Sin notificaciones configuradas no envía una URL vacía al proveedor")
    void sinNotificacionesConfiguradasNoEnviaUnaUrlVacia() {
        dadoQueNoHayUrlDeNotificaciones();

        cuandoSeCreaElContrato();

        entoncesNoSeMandoLaUrlDeNotificaciones();
    }

    // --- consultar ---

    @ParameterizedTest
    @ValueSource(ints = {199, 300, 401, 429, 500})
    @DisplayName("Una respuesta HTTP no exitosa nunca se interpreta como contratación aprobada")
    void unaRespuestaHttpNoExitosaNuncaEsUnaContratacionAprobada(int estado) {
        mercadoPago.responderSiempreConEstado(estado);

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Una falla de red conserva un mensaje seguro sin exponer el token")
    void unaFallaDeRedNoExponeElToken() {
        mercadoPago.fallarCon(new IOException("token privado"));

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
        entoncesElErrorNoMenciona("token privado");
    }

    @Test
    @DisplayName("Una interrupción restaura la marca del hilo y no concede acceso")
    void unaInterrupcionRestauraLaMarcaDelHilo() {
        mercadoPago.fallarCon(new InterruptedException());

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
        entoncesElHiloQuedoInterrumpido();
    }

    @Test
    @DisplayName("JSON inválido no permite deducir un pago exitoso")
    void unJsonInvalidoNoPermiteDeducirUnPagoExitoso() {
        mercadoPago.responderSiempreCon("no es json");

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @CsvSource({"collector_id,otro", "application_id,otra", "external_reference,invalida",
            "last_modified,invalida", "id,''", "init_point,https://otro.test/checkout"})
    @DisplayName("Rechaza contratos ajenos, incompletos o con una redirección externa")
    void rechazaContratosAjenosIncompletosOConRedireccionExterna(String campo, String valor) {
        dadoUnContratoCon(campo, valor);

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Acepta un contrato sin enlace y sin próxima fecha mientras se confirma la baja")
    void aceptaUnContratoSinEnlaceNiProximaFecha() {
        dadoUnContratoSin("init_point", "next_payment_date");

        cuandoSeConsultaElContrato();

        entoncesElContratoNoTieneEnlace();
        entoncesElContratoNoTieneProximoCobro();
    }

    @Test
    @DisplayName("Acepta próxima fecha nula al cerrar la renovación")
    void aceptaProximaFechaNulaAlCerrarLaRenovacion() {
        dadoUnContratoCon("next_payment_date", "nulo");

        cuandoSeConsultaElContrato();

        entoncesElContratoNoTieneProximoCobro();
    }

    @Test
    @DisplayName("Un importe remoto malformado no se convierte silenciosamente en cero")
    void unImporteMalformadoNoSeConvierteEnCero() {
        dadoUnContratoConImporte("no es un importe");

        cuandoSeIntentaConsultarElContrato();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Un identificador que intenta cambiar la URL es rechazado antes de llamar a la API")
    void unIdentificadorQueCambiaLaUrlSeRechaza() {
        cuandoSeIntenta(() -> adapter.consultar("../otro?token=secreto"));

        entoncesSeRechazaLaRegla();
    }

    // --- buscar ---

    @Test
    @DisplayName("La búsqueda por referencia recupera un contrato único consultando sus datos completos")
    void laBusquedaRecuperaUnContratoUnico() {
        dadoQueLaBusquedaEncuentra(Map.of("id", CONTRATO));

        entoncesLaBusquedaDevuelveUnContrato();
    }

    @Test
    @DisplayName("Una búsqueda sin contrato no provoca otra creación")
    void unaBusquedaSinContratoNoDevuelveNada() {
        dadoQueLaBusquedaEncuentra();

        entoncesLaBusquedaNoDevuelveNada();
    }

    @Test
    @DisplayName("Dos contratos con la misma referencia requieren revisión en vez de elegir uno arbitrario")
    void dosContratosConLaMismaReferenciaRequierenRevision() {
        dadoQueLaBusquedaEncuentra(Map.of("id", "uno"), Map.of("id", "dos"));

        cuandoSeIntenta(() -> adapter.buscar(RENOVACION_ID));

        entoncesFallaLaPasarela();
    }

    // --- cancelar ---

    @Test
    @DisplayName("La baja se solicita al identificador del proveedor sin cobrar otra vez")
    void laBajaSeSolicitaAlProveedor() {
        cuandoSeCancelaElContrato();

        entoncesSePidioCancelarConUnPut();
    }

    // --- listar pagos ---

    @Test
    @DisplayName("Consulta cada pago real de las facturas y conserva sus fechas y estado verificados")
    void consultaCadaPagoRealDeLasFacturas() {
        dadoUnaFacturaConSuPago(pago());

        cuandoSeListanLosPagos();

        entoncesHayPagos(1);
        entoncesElPagoEs("456", "123", "approved");
        entoncesElPagoEsDelContratoPor("14900", "ARS");
        entoncesElPagoSeDebitoYModificoAhora();
    }

    @ParameterizedTest
    @ValueSource(strings = {"approved", "rejected"})
    @DisplayName("Reconoce el formato observado de Suscripciones verificando contrato, factura y vendedor de prueba")
    void reconoceElFormatoActualDeSuscripciones(String estado) {
        mercadoPago.responderConUnPagoDeSuscripcion(estado);

        cuandoSeListanLosPagos();

        entoncesHayPagos(1);
        entoncesElPagoEs("factura-recurrente", "123", estado);
        entoncesElPagoEsDelContratoPor("14900", "ARS");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " "})
    @DisplayName("Sin correo registra el rechazo sólo cuando el comprador coincide con el contrato de prueba")
    void sinCorreoRegistraElRechazoDelCompradorDelContrato(String correo) {
        mercadoPago.responderConUnRechazoSinCorreo(correo);

        cuandoSeListanLosPagos();

        entoncesHayPagos(1);
        entoncesElPagoEs("factura-recurrente", "123", "rejected");
    }

    @ParameterizedTest
    @ValueSource(strings = {"approved", "pending", "refunded", ""})
    @DisplayName("La excepción de correo ausente nunca admite un pago aprobado, pendiente o reembolsado")
    void laExcepcionDeCorreoAusenteSoloAdmiteRechazos(String estado) {
        mercadoPago.responderConUnRechazoSinCorreo("ausente");
        dadoQueElPagoSinCorreoQuedo(estado);

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " ", "otro"})
    @DisplayName("Sin correo rechaza un comprador ausente o diferente al de la suscripción")
    void sinCorreoRechazaUnCompradorDistintoAlDeLaSuscripcion(String comprador) {
        mercadoPago.responderConUnRechazoSinCorreo("ausente");
        dadoQueElPagoSinCorreoLoHizo(comprador);

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ausente", "nulo", "", " "})
    @DisplayName("Sin correo exige que la suscripción identifique al comprador")
    void sinCorreoExigeQueLaSuscripcionIdentifiqueAlComprador(String comprador) {
        mercadoPago.responderConUnRechazoSinCorreo("ausente");
        dadoUnContratoCon("payer_id", comprador);

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Un correo real no se trata como correo ausente para aceptar un rechazo de prueba")
    void unCorreoRealNoSeTrataComoCorreoAusente() {
        mercadoPago.responderConUnRechazoSinCorreo("persona@example.com");

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Sin correo sigue exigiendo al vendedor configurado")
    void sinCorreoSigueExigiendoAlVendedorConfigurado() {
        mercadoPago.responderConUnRechazoSinCorreo("ausente");
        mercadoPago.responderConElVendedor(Map.of("id", 999, "tags", List.of("test_user")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Sin correo sigue exigiendo la etiqueta de cuenta de prueba del vendedor")
    void sinCorreoSigueExigiendoLaEtiquetaDeCuentaDePrueba() {
        mercadoPago.responderConUnRechazoSinCorreo("ausente");
        mercadoPago.responderConElVendedor(Map.of("id", 100, "tags", List.of("normal")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un vendedor sin la etiqueta de cuenta de prueba")
    void elFormatoNuevoRechazaUnVendedorSinEtiquetaDePrueba() {
        dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        mercadoPago.responderConElVendedor(Map.of("id", 100, "tags", List.of("normal")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un vendedor que no es el configurado")
    void elFormatoNuevoRechazaUnVendedorQueNoEsElConfigurado() {
        dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        mercadoPago.responderConElVendedor(Map.of("id", 999, "tags", List.of("test_user")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un vendedor sin etiquetas")
    void elFormatoNuevoRechazaUnVendedorSinEtiquetas() {
        dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        mercadoPago.responderConElVendedor(Map.of("id", 100));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un comprador con correo real")
    void elFormatoNuevoRechazaUnCompradorConCorreoReal() {
        Map<String, Object> pago = dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        pago.put("payer", Map.of("email", "persona@example.com"));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago sin comprador")
    void elFormatoNuevoRechazaUnPagoSinComprador() {
        Map<String, Object> pago = dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        pago.remove("payer");

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago vinculado a otra suscripción")
    void elFormatoNuevoRechazaUnPagoDeOtraSuscripcion() {
        dadoUnPagoDeSuscripcionCon("transaction_data", Map.of("subscription_id", "otro-contrato"));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago sin aplicación ni suscripción vinculada")
    void elFormatoNuevoRechazaUnPagoSinAplicacionNiSuscripcion() {
        dadoUnPagoDeSuscripcionCon("transaction_data", Map.of());

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago sin referencias de factura")
    void elFormatoNuevoRechazaUnPagoSinReferencias() {
        dadoUnPagoDeSuscripcionCon("references", List.of());

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago con dos facturas recurrentes")
    void elFormatoNuevoRechazaUnPagoConDosFacturasRecurrentes() {
        dadoUnPagoDeSuscripcionCon("references", List.of(
                Map.of("type", "RECURRING_INVOICE", "id", "una"), Map.of("type", "RECURRING_INVOICE", "id", "otra")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza una factura recurrente sin identificador")
    void elFormatoNuevoRechazaUnaFacturaRecurrenteSinIdentificador() {
        dadoUnPagoDeSuscripcionCon("references", List.of(Map.of("type", "RECURRING_INVOICE")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago que sólo referencia una preferencia")
    void elFormatoNuevoRechazaUnPagoQueSoloReferenciaUnaPreferencia() {
        dadoUnPagoDeSuscripcionCon("references", List.of(Map.of("type", "PREFERENCE", "id", "preferencia")));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un contrato con otro identificador")
    void elFormatoNuevoRechazaUnContratoConOtroIdentificador() {
        dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        dadoUnContratoCon("id", "otro-contrato");

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El formato nuevo rechaza un pago de otra aplicación")
    void elFormatoNuevoRechazaUnPagoDeOtraAplicacion() {
        Map<String, Object> pago = dadoUnPagoDeSuscripcionConLaInteraccion(interaccionDeSuscripcion());
        pago.put("application_id", "otra-aplicacion");

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @ValueSource(strings = {"factura-recurrente", "456"})
    @DisplayName("Conserva la identidad de factura vinculada al pago aunque la búsqueda incluya un id público distinto")
    void conservaLaIdentidadDeFacturaVinculadaAlPago(String idPublico) {
        mercadoPago.responderConUnPagoDeSuscripcion("approved");
        dadoUnaFacturaConElId(idPublico);

        cuandoSeListanLosPagos();

        entoncesElPagoEs("factura-recurrente", "123", "approved");
    }

    @Test
    @DisplayName("Un identificador de aplicación nulo usa el contrato y la suscripción vinculada al pago")
    void unaAplicacionNulaUsaElContratoYLaSuscripcion() {
        mercadoPago.responderConUnPagoDeSuscripcion("approved");
        dadoUnPagoDeSuscripcionSinAplicacion();

        cuandoSeListanLosPagos();

        entoncesHayPagos(1);
    }

    @Test
    @DisplayName("Un contrato de otra referencia no respalda un pago aunque la factura y el pago coincidan")
    void unContratoDeOtraReferenciaNoRespaldaUnPago() {
        dadoUnaFacturaConSuPago(pago());
        dadoUnContratoCon("external_reference", OTRA_REFERENCIA);

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El modo productivo admite un pago live verificado sin depender de una cuenta de prueba")
    void elModoProductivoAdmiteUnPagoLiveVerificado() {
        dadoQueEstaEnProduccion();
        dadoUnaFacturaConSuPago(unPagoCon("live_mode", true));

        cuandoSeListanLosPagos();

        entoncesHayPagos(1);
    }

    @Test
    @DisplayName("El modo productivo no activa acceso con un pago marcado como no live")
    void elModoProductivoRechazaUnPagoNoLive() {
        dadoQueEstaEnProduccion();
        dadoUnaFacturaConSuPago(pago());

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @ParameterizedTest
    @CsvSource({"collector_id,otro", "application_id,otra", "external_reference,00000000-0000-0000-0000-000000000000",
            "transaction_amount,1", "currency_id,USD", "id,otro"})
    @DisplayName("Un pago con cuenta, referencia, importe, moneda o identidad diferentes no es válido")
    void unPagoQueNoCoincideNoEsValido(String campo, String valor) {
        dadoUnaFacturaConSuPago(unPagoCon(campo, valor));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("El entorno de pruebas rechaza pagos reales aunque estén aprobados")
    void elEntornoDePruebasRechazaPagosReales() {
        dadoUnaFacturaConSuPago(unPagoCon("live_mode", true));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Un pago sin ambiente explícito no es aceptado")
    void unPagoSinAmbienteExplicitoNoEsAceptado() {
        dadoUnaFacturaConSuPago(unPagoSin("live_mode"));

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Una factura de otra suscripción no se aplica al contrato consultado")
    void unaFacturaDeOtraSuscripcionNoSeAplica() {
        dadoUnaFacturaDeOtraSuscripcion();
        mercadoPago.responderConElPago(pago());

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Las facturas todavía sin pago no habilitan acceso")
    void lasFacturasSinPagoNoHabilitanAcceso() {
        dadoFacturasSinPago();

        cuandoSeListanLosPagos();

        entoncesHayPagos(0);
    }

    @Test
    @DisplayName("Recorre todas las páginas sin imponer un límite rechazado por Mercado Pago")
    void recorreTodasLasPaginas() {
        dadoDosPaginasDeFacturas();

        cuandoSeListanLosPagos();

        entoncesHayPagos(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"results\":[]}", "{\"results\":{},\"paging\":{\"total\":0}}",
            "{\"results\":[],\"paging\":{\"total\":1}}"})
    @DisplayName("Una paginación incompleta o inválida no se interpreta como todos los pagos conciliados")
    void unaPaginacionInvalidaNoSeInterpretaComoConciliada(String json) {
        mercadoPago.responderSiempreCon(json);

        cuandoSeIntentanListarLosPagos();

        entoncesFallaLaPasarela();
    }

    @Test
    @DisplayName("Sin facturas devuelve una lista vacía")
    void sinFacturasDevuelveUnaListaVacia() {
        mercadoPago.responderConLasFacturas(List.of());

        cuandoSeListanLosPagos();

        entoncesHayPagos(0);
    }

    // --- identificar el contrato de una notificación ---

    @Test
    @DisplayName("Una notificación del contrato lo identifica por su propio id")
    void unaNotificacionDelContratoLoIdentificaPorSuId() {
        entoncesLaNotificacionIdentificaAlContrato("subscription_preapproval", CONTRATO, CONTRATO);
    }

    @Test
    @DisplayName("Una notificación de factura identifica el contrato de la factura")
    void unaNotificacionDeFacturaIdentificaSuContrato() {
        mercadoPago.responderA("GET /authorized_payments/456", factura());

        entoncesLaNotificacionIdentificaAlContrato("subscription_authorized_payment", "456", CONTRATO);
    }

    @Test
    @DisplayName("Una notificación de pago identifica el contrato de la factura que lo cobró")
    void unaNotificacionDePagoIdentificaElContratoDeSuFactura() {
        dadoQueElPagoTieneLasFacturas(factura());

        entoncesLaNotificacionIdentificaAlContrato("payment", RECURSO, CONTRATO);
    }

    @Test
    @DisplayName("Una notificación de otro tipo no identifica ningún contrato")
    void unaNotificacionDeOtroTipoNoIdentificaNingunContrato() {
        entoncesLaNotificacionIdentificaAlContrato("desconocido", RECURSO, "");
    }

    @Test
    @DisplayName("Un pago sin factura vinculada no se asigna a un negocio por aproximación")
    void unPagoSinFacturaNoSeAsignaPorAproximacion() {
        dadoQueElPagoTieneLasFacturas();

        entoncesLaNotificacionIdentificaAlContrato("payment", RECURSO, "");
    }

    // --- validar la notificación ---

    @Test
    @DisplayName("Una firma HMAC válida autentica la notificación")
    void unaFirmaHmacValidaAutenticaLaNotificacion() {
        cuandoSeValidaLaNotificacion(firmaValida(), SOLICITUD, RECURSO);

        entoncesNoHuboError();
    }

    @Test
    @DisplayName("Una notificación sin firma se rechaza")
    void unaNotificacionSinFirmaSeRechaza() {
        cuandoSeValidaLaNotificacion(null, SOLICITUD, RECURSO);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una notificación sin identificador de solicitud se rechaza")
    void unaNotificacionSinSolicitudSeRechaza() {
        cuandoSeValidaLaNotificacion(firmaValida(), null, RECURSO);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una notificación sin recurso se rechaza")
    void unaNotificacionSinRecursoSeRechaza() {
        cuandoSeValidaLaNotificacion(firmaValida(), SOLICITUD, null);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Sin secreto configurado ninguna notificación es válida")
    void sinSecretoConfiguradoNingunaNotificacionEsValida() {
        dadoQueNoHaySecretoDeNotificaciones();

        cuandoSeValidaLaNotificacion(firmaValida(), SOLICITUD, RECURSO);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una firma con un tiempo que no es numérico se rechaza")
    void unaFirmaConTiempoNoNumericoSeRechaza() {
        cuandoSeValidaLaNotificacion("basura,ts=no-numerico,v1=zz", SOLICITUD, RECURSO);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una firma con un hash malformado se rechaza")
    void unaFirmaConHashMalformadoSeRechaza() {
        cuandoSeValidaLaNotificacion("ts=" + FIRMA_TIEMPO + ",v1=no-valida", SOLICITUD, RECURSO);

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una firma de otro recurso se rechaza")
    void unaFirmaDeOtroRecursoSeRechaza() {
        cuandoSeValidaLaNotificacion(firmaValida(), SOLICITUD, "456");

        entoncesLaNotificacionEsInvalida();
    }

    @Test
    @DisplayName("Una falla criptográfica nunca acepta la notificación")
    void unaFallaCriptograficaNuncaAceptaLaNotificacion() {
        cuandoSeValidaLaNotificacionSinAlgoritmoHmac();

        entoncesFallaLaValidacionCriptografica();
    }

    // --- disponibilidad ---

    @Test
    @DisplayName("Sin configuración el adaptador informa que la contratación aún no está disponible")
    void sinConfiguracionLaContratacionNoEstaDisponible() {
        dadoQueMercadoPagoEstaDeshabilitado();

        cuandoSeIntenta(adapter::exigirDisponible);

        entoncesNoEstaDisponible();
        entoncesSeRechazaLaRegla();
    }

    // --- dado ---

    private void dadoQueNoHayUrlDeNotificaciones() {
        properties.setNotificationUrl("");
    }

    private void dadoQueNoHaySecretoDeNotificaciones() {
        properties.setWebhookSecret("");
    }

    private void dadoQueEstaEnProduccion() {
        properties.setProduccion(true);
    }

    private void dadoQueMercadoPagoEstaDeshabilitado() {
        properties.setHabilitado(false);
    }

    private void dadoUnContratoCon(String campo, String valor) {
        Map<String, Object> modificado = contrato();
        conCampo(modificado, campo, valor);
        mercadoPago.responderConElContrato(modificado);
    }

    private void dadoUnContratoSin(String... campos) {
        Map<String, Object> modificado = contrato();
        List.of(campos).forEach(modificado::remove);
        mercadoPago.responderConElContrato(modificado);
    }

    private void dadoUnContratoConImporte(String importe) {
        Map<String, Object> modificado = contrato();
        Map<String, Object> conImporte = new HashMap<>(recurrencia());
        conImporte.put("transaction_amount", importe);
        modificado.put("auto_recurring", conImporte);
        mercadoPago.responderConElContrato(modificado);
    }

    @SafeVarargs
    private void dadoQueLaBusquedaEncuentra(Map<String, Object>... contratos) {
        mercadoPago.responderA("GET /preapproval/search?q=" + RENOVACION_ID + "&limit=2",
                Map.of("results", List.of(contratos)));
    }

    @SafeVarargs
    private void dadoQueElPagoTieneLasFacturas(Map<String, Object>... facturas) {
        mercadoPago.responderA("GET /authorized_payments/search?payment_id=" + RECURSO,
                Map.of("results", List.of(facturas)));
    }

    private void dadoUnaFacturaConSuPago(Map<String, Object> pago) {
        mercadoPago.responderConLasFacturas(List.of(factura()));
        mercadoPago.responderConElPago(pago);
    }

    private void dadoUnaFacturaConElId(String id) {
        Map<String, Object> conId = factura();
        conId.put("id", id);
        mercadoPago.responderConLasFacturas(List.of(conId));
    }

    private void dadoUnaFacturaDeOtraSuscripcion() {
        Map<String, Object> ajena = factura();
        ajena.put("preapproval_id", "otro");
        mercadoPago.responderConLasFacturas(List.of(ajena));
    }

    private void dadoFacturasSinPago() {
        Map<String, Object> sinId = factura();
        sinId.put("payment", Map.of());
        Map<String, Object> conIdNulo = factura();
        conIdNulo.put("payment", compradorSinCorreo("nulo"));
        mercadoPago.responderConLasFacturas(List.of(sinId, conIdNulo));
    }

    private void dadoDosPaginasDeFacturas() {
        mercadoPago.responderA("GET /authorized_payments/search?preapproval_id=" + CONTRATO + "&offset=0",
                Map.of("paging", Map.of("total", 2), "results", List.of(factura())));
        mercadoPago.responderA("GET /authorized_payments/search?preapproval_id=" + CONTRATO + "&offset=1",
                Map.of("paging", Map.of("total", 2), "results", List.of(factura())));
        mercadoPago.responderConElPago(pago());
    }

    private void dadoQueElPagoSinCorreoQuedo(String estado) {
        Map<String, Object> pago = rechazoSinCorreo("ausente");
        pago.put("status", estado);
        mercadoPago.responderConElPago(pago);
    }

    private void dadoQueElPagoSinCorreoLoHizo(String comprador) {
        Map<String, Object> pago = rechazoSinCorreo("ausente");
        pago.put("payer", compradorSinCorreo(comprador));
        mercadoPago.responderConElPago(pago);
    }

    private Map<String, Object> dadoUnPagoDeSuscripcionConLaInteraccion(Map<String, Object> interaccion) {
        mercadoPago.responderConUnPagoDeSuscripcion("approved");
        Map<String, Object> pago = pagoDeSuscripcionConInteraccion(interaccion);
        mercadoPago.responderConElPago(pago);
        return pago;
    }

    private void dadoUnPagoDeSuscripcionCon(String dato, Object valor) {
        Map<String, Object> interaccion = interaccionDeSuscripcion();
        interaccion.put(dato, valor);
        dadoUnPagoDeSuscripcionConLaInteraccion(interaccion);
    }

    private void dadoUnPagoDeSuscripcionSinAplicacion() {
        Map<String, Object> pago = pagoDeSuscripcion("approved");
        pago.put("application_id", null);
        mercadoPago.responderConElPago(pago);
    }

    private static Map<String, Object> unPagoCon(String campo, Object valor) {
        Map<String, Object> pago = pago();
        pago.put(campo, valor);
        return pago;
    }

    private static Map<String, Object> unPagoSin(String campo) {
        Map<String, Object> pago = pago();
        pago.remove(campo);
        return pago;
    }

    // --- cuando ---

    private void cuandoSeCreaElContrato() {
        contrato = adapter.crear(renovacion(), "cliente@example.test");
    }

    private void cuandoSeConsultaElContrato() {
        contrato = adapter.consultar(CONTRATO);
    }

    private void cuandoSeIntentaConsultarElContrato() {
        cuandoSeIntenta(this::cuandoSeConsultaElContrato);
    }

    private void cuandoSeCancelaElContrato() {
        adapter.cancelar(CONTRATO);
    }

    private void cuandoSeListanLosPagos() {
        pagos = adapter.listarPagos(CONTRATO);
    }

    private void cuandoSeIntentanListarLosPagos() {
        cuandoSeIntenta(this::cuandoSeListanLosPagos);
    }

    private void cuandoSeValidaLaNotificacion(String firma, String solicitud, String recurso) {
        cuandoSeIntenta(() -> adapter.validarNotificacion(firma, solicitud, recurso));
    }

    private void cuandoSeValidaLaNotificacionSinAlgoritmoHmac() {
        String firma = firmaValida();
        try (var estatico = mockStatic(Mac.class)) {
            estatico.when(() -> Mac.getInstance("HmacSHA256")).thenThrow(new NoSuchAlgorithmException());
            cuandoSeValidaLaNotificacion(firma, SOLICITUD, RECURSO);
        }
    }

    private void cuandoSeIntenta(ThrowingCallable accion) {
        error = catchThrowable(accion);
    }

    // --- entonces ---

    private JsonNode cuerpoEnviado() {
        return cuerpoDe(mercadoPago.unicaPeticionEnviada());
    }

    private void entoncesSeLlamoA(String url) {
        assertThat(mercadoPago.unicaPeticionEnviada().uri().toString()).isEqualTo(url);
    }

    private void entoncesSeAutenticoConElTokenYEsperoHasta(Duration espera) {
        HttpRequest peticion = mercadoPago.unicaPeticionEnviada();
        assertThat(peticion.headers().firstValue("Authorization")).contains("Bearer token-de-prueba");
        assertThat(peticion.timeout()).contains(espera);
    }

    private void entoncesSePidioUnContratoPendientePara(String email) {
        JsonNode cuerpo = cuerpoEnviado();
        assertThat(cuerpo.path("external_reference").asString()).isEqualTo(RENOVACION_ID.toString());
        assertThat(cuerpo.path("payer_email").asString()).isEqualTo(email);
        assertThat(cuerpo.path("status").asString()).isEqualTo("pending");
    }

    private void entoncesSePidioCobrarCadaTreintaDias(String importe, String moneda, String inicio) {
        JsonNode recurrencia = cuerpoEnviado().path("auto_recurring");
        assertThat(recurrencia.path("frequency").asInt()).isEqualTo(30);
        assertThat(recurrencia.path("frequency_type").asString()).isEqualTo("days");
        assertThat(recurrencia.path("transaction_amount").decimalValue()).isEqualByComparingTo(importe);
        assertThat(recurrencia.path("currency_id").asString()).isEqualTo(moneda);
        assertThat(recurrencia.path("start_date").asString()).isEqualTo(inicio);
    }

    private void entoncesNoSeMandoUnaTarjeta() {
        assertThat(cuerpoEnviado().has("card_token_id")).isFalse();
    }

    private void entoncesNoSeMandoLaUrlDeNotificaciones() {
        assertThat(cuerpoEnviado().has("notification_url")).isFalse();
    }

    private void entoncesSePidioCancelarConUnPut() {
        assertThat(mercadoPago.unicaPeticionEnviada().method()).isEqualTo("PUT");
        assertThat(cuerpoEnviado().path("status").asString()).isEqualTo("cancelled");
    }

    private void entoncesElContratoEsDeLaRenovacionPor(String importe) {
        assertThat(contrato.referencia()).isEqualTo(RENOVACION_ID);
        assertThat(contrato.importe()).isEqualByComparingTo(importe);
    }

    private void entoncesElContratoEmpiezaAhoraYCobraEnTreintaDias() {
        assertThat(contrato.inicioEn()).isEqualTo(AHORA);
        assertThat(contrato.proximoCobroEn()).isEqualTo(AHORA.plusDays(30));
    }

    private void entoncesElContratoNoTieneEnlace() {
        assertThat(contrato.enlace()).isEmpty();
    }

    private void entoncesElContratoNoTieneProximoCobro() {
        assertThat(contrato.proximoCobroEn()).isNull();
    }

    private void entoncesLaBusquedaDevuelveUnContrato() {
        assertThat(adapter.buscar(RENOVACION_ID)).isPresent();
    }

    private void entoncesLaBusquedaNoDevuelveNada() {
        assertThat(adapter.buscar(RENOVACION_ID)).isEmpty();
    }

    private void entoncesHayPagos(int cantidad) {
        assertThat(pagos).hasSize(cantidad);
    }

    private void entoncesElPagoEs(String facturaId, String pagoId, String estado) {
        PagoRemoto pago = pagos.getFirst();
        assertThat(pago.facturaId()).isEqualTo(facturaId);
        assertThat(pago.pagoId()).isEqualTo(pagoId);
        assertThat(pago.estado()).isEqualTo(estado);
    }

    private void entoncesElPagoEsDelContratoPor(String importe, String moneda) {
        PagoRemoto pago = pagos.getFirst();
        assertThat(pago.contratoId()).isEqualTo(CONTRATO);
        assertThat(pago.referencia()).isEqualTo(RENOVACION_ID);
        assertThat(pago.importe()).isEqualByComparingTo(importe);
        assertThat(pago.moneda()).isEqualTo(moneda);
    }

    private void entoncesElPagoSeDebitoYModificoAhora() {
        assertThat(pagos.getFirst().debitoEn()).isEqualTo(AHORA);
        assertThat(pagos.getFirst().modificadoEn()).isEqualTo(AHORA);
    }

    private void entoncesLaNotificacionIdentificaAlContrato(String tipo, String recurso, String contratoId) {
        assertThat(adapter.identificarContrato(tipo, recurso)).isEqualTo(contratoId);
    }

    private void entoncesNoHuboError() {
        assertThat(error).isNull();
    }

    private void entoncesFallaLaPasarela() {
        assertThat(error).isInstanceOf(PasarelaPagoException.class);
    }

    private void entoncesElErrorNoMenciona(String texto) {
        assertThat(error).hasMessageNotContaining(texto);
    }

    private void entoncesElHiloQuedoInterrumpido() {
        assertThat(Thread.interrupted()).isTrue();
    }

    private void entoncesSeRechazaLaRegla() {
        assertThat(error).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesLaNotificacionEsInvalida() {
        assertThat(error).isInstanceOf(NotificacionPagoInvalidaException.class);
    }

    private void entoncesFallaLaValidacionCriptografica() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesNoEstaDisponible() {
        assertThat(adapter.disponible()).isFalse();
    }
}
