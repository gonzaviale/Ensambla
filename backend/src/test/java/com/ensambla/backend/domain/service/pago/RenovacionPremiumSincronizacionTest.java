package com.ensambla.backend.domain.service.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.ContratoRemoto;
import com.ensambla.backend.domain.model.pago.PagoPremium;
import com.ensambla.backend.domain.model.pago.PagoRemoto;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.PROVEEDOR_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.pago;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** La sincronización de una renovación con el proveedor de pagos que hace {@link RenovacionPremiumService}. */
@ExtendWith(MockitoExtension.class)
class RenovacionPremiumSincronizacionTest {

    private static final PlanId PREMIUM_ID = PlanId.de("b0000000-0000-0000-0000-000000000001");

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock
    private PagoPremiumRepository pagoPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private PlanRepository planRepository;

    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private SuscripcionRepository suscripcionRepository;

    private RenovacionPremiumService renovacionPremiumService;
    private RenovacionPremium renovacion;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        renovacionPremiumService = new RenovacionPremiumService(renovacionPremiumRepository, usoPlanRepository,
                pasarelaSuscripcionesPort, pagoPremiumRepository, planRepository, suscripcionRepository,
                new TransaccionDirecta());
    }

    @Test
    @DisplayName("Un contrato en preparación sin resultado remoto no se crea de nuevo")
    void unContratoEnPreparacionSinResultadoRemotoNoSeCreaDeNuevo() {
        dadoUnContratoEnPreparacionSinResultadoRemoto();

        cuandoSeSincroniza();

        entoncesSeBuscaElContratoSinCrearloNiTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Un pago con referencia ajena no habilita acceso")
    void unPagoConReferenciaAjenaNoHabilitaAcceso() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoConReferenciaAjena();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Un pago con contrato ajeno no habilita acceso")
    void unPagoConContratoAjenoNoHabilitaAcceso() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoConContratoAjeno();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Un pago con importe ajeno no habilita acceso")
    void unPagoConImporteAjenoNoHabilitaAcceso() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoConImporteAjeno();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Un pago con moneda ajena no habilita acceso")
    void unPagoConMonedaAjenaNoHabilitaAcceso() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoConMonedaAjena();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Un pago con fecha de inicio ajena no habilita acceso")
    void unPagoConFechaDeInicioAjenaNoHabilitaAcceso() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoConFechaDeInicioAjena();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones();
    }

    @Test
    @DisplayName("Una factura existente no puede cambiar de negocio")
    void unaFacturaExistenteNoPuedeCambiarDeNegocio() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoAprobado();
        dadoQueLaFacturaYaEstaGuardadaEnOtroContrato();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinGuardarElPago();
    }

    @Test
    @DisplayName("Una factura existente no puede cambiar de período")
    void unaFacturaExistenteNoPuedeCambiarDePeriodo() {
        dadoUnaRenovacionSincronizada();
        dadoUnPagoAprobado();
        dadoQueLaFacturaYaEstaGuardadaEnOtroPeriodo();

        cuandoSeIntentaSincronizar();

        entoncesLaPasarelaLoRechazaSinGuardarElPago();
    }

    @Test
    @DisplayName("Una suscripción Premium heredada no se degrada sin historial de pagos de esta integración")
    void unaSuscripcionPremiumHeredadaNoSeDegradaSinHistorialDePagosDeEstaIntegracion() {
        dadoUnaRenovacionSincronizada();
        dadoQueElProveedorNoTienePagos();
        dadoUnaSuscripcionPremiumHeredadaSinHistorialDePagos();

        cuandoSeSincroniza();

        entoncesNoSeGuardaLaSuscripcion();
    }

    @Test
    @DisplayName("Una prueba sin pagos conserva su vigencia al conciliar")
    void unaPruebaSinPagosConservaSuVigenciaAlConciliar() {
        dadoUnaRenovacionSincronizada();
        dadoQueElProveedorNoTienePagos();
        dadoUnaPruebaVigente();

        cuandoSeSincroniza();

        entoncesLaPruebaConservaSuVigenciaSinConsultarPlanes();
    }

    // --- dado ---

    private void dadoUnContratoEnPreparacionSinResultadoRemoto() {
        renovacion = renovacion();
        renovacion.setProveedorId(null);
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
        when(pasarelaSuscripcionesPort.buscar(RENOVACION_ID)).thenReturn(Optional.empty());
    }

    private void dadoUnaRenovacionSincronizada() {
        renovacion = renovacion();
        when(renovacionPremiumRepository.findById(RENOVACION_ID)).thenReturn(Optional.of(renovacion));
        when(renovacionPremiumRepository.findNegocioIdById(RENOVACION_ID)).thenReturn(Optional.of(NEGOCIO_ID));
        when(renovacionPremiumRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pasarelaSuscripcionesPort.consultar(PROVEEDOR_ID)).thenReturn(contratoAutorizado(renovacion));
    }

    private void dadoUnPagoAprobado() {
        dadoLosPagos(pagoBase());
    }

    private void dadoUnPagoConReferenciaAjena() {
        PagoRemoto base = pagoBase();
        dadoLosPagos(new PagoRemoto(base.facturaId(), base.pagoId(), base.contratoId(), RenovacionId.nuevo(),
                base.importe(), base.moneda(), base.estado(), AHORA, AHORA));
    }

    private void dadoUnPagoConContratoAjeno() {
        PagoRemoto base = pagoBase();
        dadoLosPagos(new PagoRemoto(base.facturaId(), base.pagoId(), "otro", base.referencia(), base.importe(),
                base.moneda(), base.estado(), AHORA, AHORA));
    }

    private void dadoUnPagoConImporteAjeno() {
        PagoRemoto base = pagoBase();
        dadoLosPagos(new PagoRemoto(base.facturaId(), base.pagoId(), base.contratoId(), base.referencia(),
                BigDecimal.ONE, base.moneda(), base.estado(), AHORA, AHORA));
    }

    private void dadoUnPagoConMonedaAjena() {
        PagoRemoto base = pagoBase();
        dadoLosPagos(new PagoRemoto(base.facturaId(), base.pagoId(), base.contratoId(), base.referencia(),
                base.importe(), "USD", base.estado(), AHORA, AHORA));
    }

    private void dadoUnPagoConFechaDeInicioAjena() {
        PagoRemoto base = pagoBase();
        dadoLosPagos(new PagoRemoto(base.facturaId(), base.pagoId(), base.contratoId(), base.referencia(),
                base.importe(), base.moneda(), base.estado(), AHORA.minusSeconds(1), AHORA));
    }

    private void dadoQueLaFacturaYaEstaGuardadaEnOtroContrato() {
        dadoUnaFacturaGuardada(RenovacionId.nuevo(), AHORA);
    }

    private void dadoQueLaFacturaYaEstaGuardadaEnOtroPeriodo() {
        dadoUnaFacturaGuardada(RENOVACION_ID, AHORA.minusDays(1));
    }

    private void dadoUnaFacturaGuardada(RenovacionId renovacionId, OffsetDateTime inicio) {
        when(pagoPremiumRepository.findByFacturaId("factura")).thenReturn(Optional.of(PagoPremium.builder()
                .renovacionId(renovacionId).inicioEn(inicio).build()));
    }

    private void dadoQueElProveedorNoTienePagos() {
        dadoLosPagos();
    }

    private void dadoLosPagos(PagoRemoto... pagos) {
        when(pasarelaSuscripcionesPort.listarPagos(PROVEEDOR_ID)).thenReturn(List.of(pagos));
    }

    private void dadoUnaSuscripcionPremiumHeredadaSinHistorialDePagos() {
        dadoElRecalculoDe(suscripcion(EstadoSuscripcion.ACTIVA));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        when(pagoPremiumRepository.existeByNegocioId(NEGOCIO_ID)).thenReturn(false);
    }

    private void dadoUnaPruebaVigente() {
        dadoElRecalculoDe(suscripcion(EstadoSuscripcion.PRUEBA));
    }

    private void dadoElRecalculoDe(Suscripcion suscripcion) {
        when(pagoPremiumRepository.findVigente(NEGOCIO_ID, AHORA)).thenReturn(Optional.empty());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
    }

    // --- cuando ---

    private void cuandoSeSincroniza() {
        renovacionPremiumService.sincronizar(RENOVACION_ID, AHORA);
    }

    private void cuandoSeIntentaSincronizar() {
        rechazo = catchThrowable(this::cuandoSeSincroniza);
    }

    // --- entonces ---

    private void entoncesSeBuscaElContratoSinCrearloNiTocarPagosNiSuscripciones() {
        verify(pasarelaSuscripcionesPort).buscar(RENOVACION_ID);
        verify(renovacionPremiumRepository, never()).save(any());
        verifyNoInteractions(pagoPremiumRepository, suscripcionRepository);
    }

    private void entoncesLaPasarelaLoRechazaSinTocarPagosNiSuscripciones() {
        assertThat(rechazo).isInstanceOf(PasarelaPagoException.class);
        verifyNoInteractions(pagoPremiumRepository, suscripcionRepository);
    }

    private void entoncesLaPasarelaLoRechazaSinGuardarElPago() {
        assertThat(rechazo).isInstanceOf(PasarelaPagoException.class);
        verify(pagoPremiumRepository, never()).save(any());
    }

    private void entoncesNoSeGuardaLaSuscripcion() {
        verify(suscripcionRepository, never()).save(any());
    }

    private void entoncesLaPruebaConservaSuVigenciaSinConsultarPlanes() {
        verifyNoInteractions(planRepository);
        verify(suscripcionRepository, never()).save(any());
    }

    // --- datos ---

    private PagoRemoto pagoBase() {
        return pago(renovacion, "factura", "approved", AHORA, AHORA);
    }

    private static ContratoRemoto contratoAutorizado(RenovacionPremium renovacion) {
        return new ContratoRemoto(PROVEEDOR_ID, renovacion.getId(), renovacion.getEnlace(), "authorized",
                renovacion.getImporte(), renovacion.getMoneda(), 30, "days", renovacion.getPrimerCobroEn(),
                renovacion.getPrimerCobroEn().plusDays(30), AHORA);
    }

    private static Plan premium() {
        return Plan.builder().id(PREMIUM_ID).codigo(CodigoPlan.PREMIUM).configurado(true)
                .limitesConfigurados(true).precioMensual(IMPORTE).moneda("ARS").build();
    }

    private static Suscripcion suscripcion(EstadoSuscripcion estado) {
        return Suscripcion.builder().negocioId(NEGOCIO_ID).planId(PREMIUM_ID).estado(estado)
                .inicioEn(AHORA.minusDays(1)).build();
    }
}
