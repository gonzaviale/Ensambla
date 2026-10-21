package com.ensambla.backend.domain.useCase.pago;

import com.ensambla.backend.domain.exception.PasarelaPagoException;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.infrastructure.pago.ConciliacionPagosWorker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConciliacionPagosTest {

    private final RenovacionPremiumService renovacionPremiumService = mock(RenovacionPremiumService.class);
    private final PasarelaSuscripcionesPort pasarelaSuscripcionesPort = mock(PasarelaSuscripcionesPort.class);
    private final SincronizarRenovacionUseCase sincronizarRenovacionUseCase = mock(SincronizarRenovacionUseCase.class);
    private final Clock clock = Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC);

    @Test
    @DisplayName("La conciliación continúa con otros negocios cuando un proveedor falla y rota los intentos")
    void executeShouldContinueAfterProviderFailure() {
        UUID segundo = UUID.randomUUID();
        when(renovacionPremiumService.listarParaSincronizar()).thenReturn(List.of(RENOVACION_ID, segundo));
        doThrow(new PasarelaPagoException()).when(sincronizarRenovacionUseCase).execute(RENOVACION_ID);
        ConciliarPagosUseCase useCase = new ConciliarPagosUseCase(renovacionPremiumService,
                sincronizarRenovacionUseCase, clock);

        new ConciliacionPagosWorker(useCase).conciliar();

        verify(sincronizarRenovacionUseCase).execute(segundo);
        verify(renovacionPremiumService).marcarConsulta(RENOVACION_ID, AHORA);
        verify(renovacionPremiumService).marcarConsulta(segundo, AHORA);
    }

    @Test
    @DisplayName("Un contrato en preparación sin resultado remoto no se crea de nuevo")
    void executeShouldNotRecreateUnknownContract() {
        RenovacionPremium renovacion = renovacion();
        renovacion.setProveedorId(null);
        when(renovacionPremiumService.obtener(RENOVACION_ID)).thenReturn(renovacion);
        when(pasarelaSuscripcionesPort.buscar(RENOVACION_ID)).thenReturn(Optional.empty());
        SincronizarRenovacionUseCase useCase = new SincronizarRenovacionUseCase(renovacionPremiumService,
                pasarelaSuscripcionesPort, clock);

        useCase.execute(RENOVACION_ID);

        verify(pasarelaSuscripcionesPort).buscar(RENOVACION_ID);
        verifyNoInteractions(sincronizarRenovacionUseCase);
    }

    @Test
    @DisplayName("Una notificación de un contrato desconocido no afecta al negocio de otra cuenta")
    void executeShouldIgnoreUnknownNotification() {
        when(pasarelaSuscripcionesPort.identificarContrato("payment", "123")).thenReturn("otro");
        when(renovacionPremiumService.buscarPorProveedor("otro")).thenReturn(Optional.empty());
        RecibirNotificacionPagoUseCase useCase = new RecibirNotificacionPagoUseCase(pasarelaSuscripcionesPort,
                renovacionPremiumService, sincronizarRenovacionUseCase);

        useCase.execute("payment", "123", "firma", "solicitud");

        verify(pasarelaSuscripcionesPort).validarNotificacion("firma", "solicitud", "123");
        verifyNoInteractions(sincronizarRenovacionUseCase);
    }
}
