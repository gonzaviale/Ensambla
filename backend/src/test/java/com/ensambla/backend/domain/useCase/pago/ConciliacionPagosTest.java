package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.RenovacionId;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.PagoPremiumRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.conciliarPagos.ConciliarPagos;
import com.ensambla.backend.domain.useCase.pago.recibirNotificacionPago.RecibirNotificacionPago;
import com.ensambla.backend.infrastructure.pago.ConciliacionPagosWorker;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConciliacionPagosTest {

    private final RenovacionPremiumRepository renovacionPremiumRepository = mock(RenovacionPremiumRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort = mock(PasarelaSuscripcionesPort.class);
    private final RenovacionPremiumService renovacionPremiumService = spy(new RenovacionPremiumService(
            renovacionPremiumRepository, usoPlanRepository, pasarelaSuscripcionesPort,
            mock(PagoPremiumRepository.class), mock(PlanRepository.class), mock(SuscripcionRepository.class),
            new TransaccionDirecta()));
    private final Clock clock = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    @Test
    @DisplayName("La conciliación continúa con otros negocios cuando un proveedor falla y rota los intentos")
    void executeShouldContinueAfterProviderFailure() {
        RenovacionId segundo = RenovacionId.nuevo();
        RenovacionPremium primera = givenRenovacion(RENOVACION_ID);
        RenovacionPremium otra = givenRenovacion(segundo);
        when(renovacionPremiumRepository.listarParaSincronizar()).thenReturn(List.of(RENOVACION_ID, segundo));
        doThrow(new PasarelaPagoException()).when(renovacionPremiumService).sincronizar(RENOVACION_ID, AHORA);
        doNothing().when(renovacionPremiumService).sincronizar(segundo, AHORA);
        ConciliarPagos useCase = new ConciliarPagos(
                renovacionPremiumService, renovacionPremiumRepository, new TransaccionDirecta(), clock);

        new ConciliacionPagosWorker(useCase).conciliar();

        verify(renovacionPremiumService).sincronizar(segundo, AHORA);
        assertThat(primera.getSincronizadoEn()).isEqualTo(AHORA);
        assertThat(otra.getSincronizadoEn()).isEqualTo(AHORA);
        verify(renovacionPremiumRepository).save(primera);
        verify(renovacionPremiumRepository).save(otra);
    }

    @Test
    @DisplayName("Una notificación de un contrato desconocido no afecta al negocio de otra cuenta")
    void executeShouldIgnoreUnknownNotification() {
        when(pasarelaSuscripcionesPort.identificarContrato("payment", "123")).thenReturn("otro");
        when(renovacionPremiumRepository.findByProveedorId("otro")).thenReturn(Optional.empty());
        RecibirNotificacionPago useCase = new RecibirNotificacionPago(pasarelaSuscripcionesPort,
                renovacionPremiumRepository, renovacionPremiumService, clock);

        useCase.execute("payment", "123", "firma", "solicitud");

        verify(pasarelaSuscripcionesPort).validarNotificacion("firma", "solicitud", "123");
        verify(renovacionPremiumService, never()).sincronizar(any(), any());
    }

    // --- arrange ---
    private RenovacionPremium givenRenovacion(RenovacionId id) {
        RenovacionPremium renovacion = renovacion();
        renovacion.setId(id);
        renovacion.setSincronizadoEn(null);
        when(renovacionPremiumRepository.findNegocioIdById(id)).thenReturn(Optional.of(NEGOCIO_ID));
        when(renovacionPremiumRepository.findById(id)).thenReturn(Optional.of(renovacion));
        return renovacion;
    }
}
