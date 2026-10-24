package com.ensambla.backend.domain.useCase.pago.cancelarRenovacion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
import com.ensambla.backend.domain.useCase.pago.sincronizarRenovacion.SincronizarRenovacion;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarRenovacionTest {

    private static final UUID USUARIO_ID = UUID.fromString("b0000000-0000-0000-0000-000000000009");

    @Mock
    private NegocioService negocioService;

    @Mock
    private RenovacionPremiumService renovacionPremiumService;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock
    private SincronizarRenovacion sincronizarRenovacion;

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    private CancelarRenovacion cancelarRenovacion;

    @BeforeEach
    void setUp() {
        cancelarRenovacion = new CancelarRenovacion(negocioService, renovacionPremiumService,
                pasarelaSuscripcionesPort, sincronizarRenovacion, renovacionPremiumRepository, usoPlanRepository,
                new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Pide la cancelación con el negocio bloqueado y la confirma con la pasarela al sincronizar")
    void executeShouldRequestCancellationAndSync() {
        RenovacionPremium renovacion = renovacion();
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.of(renovacion));
        when(renovacionPremiumRepository.save(renovacion)).thenReturn(renovacion);

        ConsultarRenovacionResponse result = cancelarRenovacion.execute(USUARIO_ID);

        assertThat(renovacion.isCancelacionSolicitada()).isTrue();
        assertThat(renovacion.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(result.contrato()).isNotNull();
        var orden = inOrder(usoPlanRepository, renovacionPremiumRepository, sincronizarRenovacion);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(renovacionPremiumRepository).save(renovacion);
        orden.verify(sincronizarRenovacion).execute(RENOVACION_ID);
    }

    @Test
    @DisplayName("No se puede cancelar una renovación inexistente del negocio")
    void executeShouldRejectMissingContract() {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cancelarRenovacion.execute(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(renovacionPremiumRepository, sincronizarRenovacion);
    }
}
