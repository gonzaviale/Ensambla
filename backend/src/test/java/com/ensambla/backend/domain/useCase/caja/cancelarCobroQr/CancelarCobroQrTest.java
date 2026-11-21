package com.ensambla.backend.domain.useCase.caja.cancelarCobroQr;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCobrado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelarCobroQrTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.plusMinutes(1).toInstant(), ZoneOffset.UTC);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final UsoPlanRepository usoPlanRepository = mock(UsoPlanRepository.class);
    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class);
    private final CancelarCobroQr cancelarCobroQr = new CancelarCobroQr(negocioRepository, usoPlanRepository,
            cobroQrRepository, new TransaccionDirecta(), RELOJ);

    private CobroQrItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Cancela el cobro pendiente bajo el bloqueo del negocio y lo guarda")
    void cancelaElCobroPendienteBajoElBloqueoDelNegocioYLoGuarda() {
        dadoUnCobroGuardado(unCobroQrPendiente());

        cuandoSeCancelaElCobro();

        entoncesSeGuardaElCobroCancelado();
        entoncesSeBloqueoElNegocio();
        entoncesSeResponde("CANCELADO");
    }

    @Test
    @DisplayName("No cancela un cobro que ya se cobró")
    void noCancelaUnCobroQueYaSeCobro() {
        dadoUnCobroGuardado(unCobroQrCobrado());

        cuandoSeIntentaCancelarElCobro();

        entoncesSeRechazaPorque("El cobro con QR ya está cobrado");
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Un cobro con QR de otro negocio responde como inexistente")
    void unCobroConQrDeOtroNegocioRespondeComoInexistente() {
        dadoUnCobroDeOtroNegocio();

        cuandoSeIntentaCancelarElCobro();

        entoncesSeRespondeNoEncontrado();
        entoncesNoSeGuardaNada();
    }

    // --- dado ---
    private void dadoUnCobroGuardado(CobroQr cobro) {
        dadoUnUsuarioDelNegocio();
        when(cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID)).thenReturn(cobro);
        when(cobroQrRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnCobroDeOtroNegocio() {
        dadoUnUsuarioDelNegocio();
        when(cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("Cobro con QR no encontrado"));
    }

    private void dadoUnUsuarioDelNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    // --- cuando ---
    private void cuandoSeCancelaElCobro() {
        respuesta = cancelarCobroQr.execute(new CancelarCobroQrRequest(USUARIO_ID, COBRO_QR_ID));
    }

    private void cuandoSeIntentaCancelarElCobro() {
        rechazo = catchThrowable(this::cuandoSeCancelaElCobro);
    }

    // --- entonces ---
    private void entoncesSeGuardaElCobroCancelado() {
        ArgumentCaptor<CobroQr> guardado = ArgumentCaptor.forClass(CobroQr.class);
        verify(cobroQrRepository).save(guardado.capture());
        assertThat(guardado.getValue().estado()).isEqualTo(EstadoCobroQr.CANCELADO);
        assertThat(guardado.getValue().actualizadoEn()).isEqualTo(AHORA.plusMinutes(1));
    }

    private void entoncesSeBloqueoElNegocio() {
        verify(usoPlanRepository).bloquearNegocio(NEGOCIO_ID);
    }

    private void entoncesSeResponde(String estado) {
        assertThat(respuesta.id()).isEqualTo(COBRO_QR_ID);
        assertThat(respuesta.estado()).isEqualTo(estado);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }

    private void entoncesSeRespondeNoEncontrado() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardaNada() {
        verify(cobroQrRepository, never()).save(any());
    }
}
