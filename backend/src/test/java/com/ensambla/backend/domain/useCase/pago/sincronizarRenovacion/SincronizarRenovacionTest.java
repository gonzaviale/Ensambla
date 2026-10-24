package com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
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
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.service.plan.SuscripcionService;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.IMPORTE;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.PROVEEDOR_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.contrato;
import static com.ensambla.backend.support.PagoTestData.pago;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SincronizarRenovacionTest {

    private static final PlanId PREMIUM_ID = PlanId.de("b0000000-0000-0000-0000-000000000001");

    @Mock
    private RenovacionPremiumService renovacionPremiumService;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock
    private PagoPremiumRepository pagoPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    @Mock
    private PlanRepository planRepository;

    @Mock
    private SuscripcionRepository suscripcionRepository;

    private SincronizarRenovacion sincronizarRenovacion;

    @BeforeEach
    void setUp() {
        sincronizarRenovacion = new SincronizarRenovacion(renovacionPremiumService, pasarelaSuscripcionesPort,
                pagoPremiumRepository, usoPlanRepository, new SuscripcionService(planRepository, suscripcionRepository),
                suscripcionRepository, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Un contrato en preparación sin resultado remoto no se crea de nuevo")
    void executeShouldNotRecreateUnknownContract() {
        RenovacionPremium renovacion = renovacion();
        renovacion.setProveedorId(null);
        when(renovacionPremiumService.obtener(RENOVACION_ID)).thenReturn(renovacion);
        when(pasarelaSuscripcionesPort.buscar(RENOVACION_ID)).thenReturn(Optional.empty());

        sincronizarRenovacion.execute(RENOVACION_ID);

        verify(pasarelaSuscripcionesPort).buscar(RENOVACION_ID);
        verify(renovacionPremiumService, never()).actualizarContrato(any(), any(), any());
        verifyNoInteractions(pagoPremiumRepository, suscripcionRepository);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4})
    @DisplayName("Un pago con referencia, contrato, importe, moneda o fecha ajenos no habilita acceso")
    void executeShouldRejectMismatchedInvoice(int caso) {
        RenovacionPremium renovacion = givenRenovacionSincronizada();
        PagoRemoto base = pago(renovacion, "factura", "approved", AHORA, AHORA);
        givenPagos(new PagoRemoto(base.facturaId(), base.pagoId(), caso == 1 ? "otro" : base.contratoId(),
                caso == 0 ? RenovacionId.nuevo() : base.referencia(), caso == 2 ? BigDecimal.ONE : base.importe(),
                caso == 3 ? "USD" : base.moneda(), base.estado(), caso == 4 ? AHORA.minusSeconds(1) : AHORA, AHORA));

        assertThatThrownBy(() -> sincronizarRenovacion.execute(RENOVACION_ID))
                .isInstanceOf(PasarelaPagoException.class);

        verifyNoInteractions(pagoPremiumRepository, suscripcionRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Una factura existente no puede cambiar de negocio ni de período")
    void executeShouldRejectInvoiceReassignment(boolean otroContrato) {
        RenovacionPremium renovacion = givenRenovacionSincronizada();
        givenPagos(pago(renovacion, "factura", "approved", AHORA, AHORA));
        when(pagoPremiumRepository.findByFacturaId("factura")).thenReturn(Optional.of(PagoPremium.builder()
                .renovacionId(otroContrato ? RenovacionId.nuevo() : RENOVACION_ID)
                .inicioEn(otroContrato ? AHORA : AHORA.minusDays(1)).build()));

        assertThatThrownBy(() -> sincronizarRenovacion.execute(RENOVACION_ID))
                .isInstanceOf(PasarelaPagoException.class);
        verify(pagoPremiumRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una suscripción Premium heredada no se degrada sin historial de pagos de esta integración")
    void executeShouldPreserveLegacyPremium() {
        givenRenovacionSincronizada();
        givenPagos();
        givenRecalculo(suscripcion(EstadoSuscripcion.ACTIVA));
        when(planRepository.findByCodigo(CodigoPlan.PREMIUM)).thenReturn(Optional.of(premium()));
        when(pagoPremiumRepository.existeByNegocioId(NEGOCIO_ID)).thenReturn(false);

        sincronizarRenovacion.execute(RENOVACION_ID);

        verify(suscripcionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una prueba sin pagos conserva su vigencia al conciliar")
    void executeShouldPreserveTrial() {
        givenRenovacionSincronizada();
        givenPagos();
        givenRecalculo(suscripcion(EstadoSuscripcion.PRUEBA));

        sincronizarRenovacion.execute(RENOVACION_ID);

        verifyNoInteractions(planRepository);
        verify(suscripcionRepository, never()).save(any());
    }

    // --- arrange ---
    private RenovacionPremium givenRenovacionSincronizada() {
        RenovacionPremium renovacion = renovacion();
        when(renovacionPremiumService.obtener(RENOVACION_ID)).thenReturn(renovacion);
        when(pasarelaSuscripcionesPort.consultar(PROVEEDOR_ID)).thenReturn(contrato(renovacion, "authorized", AHORA));
        when(renovacionPremiumService.actualizarContrato(eq(RENOVACION_ID), any(), eq(AHORA))).thenReturn(renovacion);
        return renovacion;
    }

    private void givenPagos(PagoRemoto... pagos) {
        when(pasarelaSuscripcionesPort.listarPagos(PROVEEDOR_ID)).thenReturn(List.of(pagos));
    }

    private void givenRecalculo(Suscripcion suscripcion) {
        when(pagoPremiumRepository.findVigente(NEGOCIO_ID, AHORA)).thenReturn(Optional.empty());
        when(suscripcionRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(Optional.of(suscripcion));
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
