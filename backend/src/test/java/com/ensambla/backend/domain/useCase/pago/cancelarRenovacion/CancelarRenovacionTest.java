package com.ensambla.backend.domain.useCase.pago.cancelarRenovacion;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.pago.RenovacionPremium;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.PasarelaSuscripcionesPort;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.service.pago.RenovacionPremiumService;
import com.ensambla.backend.domain.useCase.pago.ConsultarRenovacionResponse;
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

import static com.ensambla.backend.support.PagoTestData.AHORA;
import static com.ensambla.backend.support.PagoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.PagoTestData.RENOVACION_ID;
import static com.ensambla.backend.support.PagoTestData.renovacion;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarRenovacionTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-0000-0000-000000000009");

    @Mock
    private NegocioRepository negocioRepository;

    @Mock
    private RenovacionPremiumService renovacionPremiumService;

    @Mock
    private PasarelaSuscripcionesPort pasarelaSuscripcionesPort;

    @Mock
    private RenovacionPremiumRepository renovacionPremiumRepository;

    @Mock
    private UsoPlanRepository usoPlanRepository;

    private CancelarRenovacion cancelarRenovacion;

    @BeforeEach
    void setUp() {
        cancelarRenovacion = new CancelarRenovacion(negocioRepository, renovacionPremiumService,
                pasarelaSuscripcionesPort, renovacionPremiumRepository, usoPlanRepository,
                new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private RenovacionPremium renovacion;
    private ConsultarRenovacionResponse respuesta;
    private Throwable error;

    @Test
    @DisplayName("Pide la cancelación con el negocio bloqueado y la confirma con la pasarela al sincronizar")
    void pideLaCancelacionConElNegocioBloqueadoYLaConfirmaConLaPasarelaAlSincronizar() {
        dadoUnaRenovacionDelNegocio();

        cuandoSeCancelaLaRenovacion();

        entoncesQuedaSolicitadaLaCancelacion();
        entoncesSeBloqueaSeGuardaYSeSincronizaEnEseOrden();
    }

    @Test
    @DisplayName("No se puede cancelar una renovación inexistente del negocio")
    void noSePuedeCancelarUnaRenovacionInexistenteDelNegocio() {
        dadoUnNegocioSinRenovacion();

        cuandoSeIntentaCancelarLaRenovacion();

        entoncesSeRechazaPorqueNoExiste();
        entoncesNoSeGuardaNiSeSincronizaNada();
    }

    // --- dado ---
    private void dadoUnaRenovacionDelNegocio() {
        renovacion = renovacion();
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.of(renovacion));
        when(renovacionPremiumRepository.save(renovacion)).thenReturn(renovacion);
    }

    private void dadoUnNegocioSinRenovacion() {
        when(renovacionPremiumService.consultar(NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSeCancelaLaRenovacion() {
        respuesta = cancelarRenovacion.execute(USUARIO_ID);
    }

    private void cuandoSeIntentaCancelarLaRenovacion() {
        error = catchThrowable(() -> cancelarRenovacion.execute(USUARIO_ID));
    }

    // --- entonces ---
    private void entoncesQuedaSolicitadaLaCancelacion() {
        assertThat(renovacion.isCancelacionSolicitada()).isTrue();
        assertThat(renovacion.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(respuesta.contrato()).isNotNull();
    }

    private void entoncesSeBloqueaSeGuardaYSeSincronizaEnEseOrden() {
        var orden = inOrder(usoPlanRepository, renovacionPremiumRepository, renovacionPremiumService);
        orden.verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
        orden.verify(renovacionPremiumRepository).save(renovacion);
        orden.verify(renovacionPremiumService).sincronizar(RENOVACION_ID, AHORA);
    }

    private void entoncesSeRechazaPorqueNoExiste() {
        assertThat(error).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNiSeSincronizaNada() {
        verifyNoInteractions(renovacionPremiumRepository);
        verify(renovacionPremiumService, never()).sincronizar(any(), any());
    }
}
