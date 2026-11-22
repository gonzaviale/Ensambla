package com.ensambla.backend.domain.service.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.ConfirmacionDeCobro;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCancelado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrMercadoPago;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CobroQrServiceTest {

    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class);
    private final CobroQrService cobroQrService = new CobroQrService(cobroQrRepository);

    private Throwable rechazo;

    @Test
    @DisplayName("Sin cobro con QR no consulta ni guarda nada")
    void sinCobroConQrNoConsultaNiGuardaNada() {
        cuandoSeConfirmaSinCobroQr();

        entoncesNoSeTocaElRepositorio();
    }

    @Test
    @DisplayName("Confirma a mano el cobro con QR cuando coincide con lo transferido")
    void confirmaAManoElCobroConQrCuandoCoincideConLoTransferido() {
        dadoUnCobroPendienteGuardado();

        cuandoSeConfirmaPor(IMPORTE);

        entoncesSeGuardaElCobroCobradoPorConfirmacionManual();
    }

    @Test
    @DisplayName("Rechaza el cobro si lo transferido no coincide con su importe")
    void rechazaElCobroSiLoTransferidoNoCoincideConSuImporte() {
        dadoUnCobroPendienteGuardado();

        cuandoSeIntentaConfirmarPor(IMPORTE.add(BigDecimal.ONE));

        entoncesSeRechazaPorque("El importe transferido no coincide con el del cobro con QR");
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Rechaza confirmar un cobro cancelado")
    void rechazaConfirmarUnCobroCancelado() {
        dadoUnCobroGuardado(unCobroQrCancelado());

        cuandoSeIntentaConfirmarPor(IMPORTE);

        entoncesSeRechazaPorque("El cobro con QR está cancelado");
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Un cobro con QR de otro negocio responde como inexistente")
    void unCobroConQrDeOtroNegocioRespondeComoInexistente() {
        dadoUnCobroDeOtroNegocio();

        cuandoSeIntentaConfirmarPor(IMPORTE);

        entoncesSeRespondeNoEncontrado();
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Acepta el cobro de Mercado Pago acreditado sin confirmarlo a mano")
    void aceptaElCobroDeMercadoPagoAcreditadoSinConfirmarloAMano() {
        dadoUnCobroGuardado(unCobroQrMercadoPago(EstadoOrdenCobroQr.COBRADO));
        cuandoSeConfirmaPor(IMPORTE);
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Rechaza registrar un cobro de Mercado Pago todavía pendiente")
    void rechazaRegistrarUnCobroDeMercadoPagoTodaviaPendiente() {
        dadoUnCobroGuardado(unCobroQrMercadoPago(EstadoOrdenCobroQr.PENDIENTE));
        cuandoSeIntentaConfirmarPor(IMPORTE);
        entoncesSeRechazaPorque("Mercado Pago todavía no acreditó el cobro con QR");
    }

    // --- dado ---
    private void dadoUnCobroPendienteGuardado() {
        dadoUnCobroGuardado(unCobroQrPendiente());
    }

    private void dadoUnCobroGuardado(CobroQr cobro) {
        when(cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID)).thenReturn(cobro);
    }

    private void dadoUnCobroDeOtroNegocio() {
        when(cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("Cobro con QR no encontrado"));
    }

    // --- cuando ---
    private void cuandoSeConfirmaSinCobroQr() {
        cobroQrService.confirmarSiHay(null, NEGOCIO_ID, IMPORTE, AHORA);
    }

    private void cuandoSeConfirmaPor(BigDecimal transferido) {
        cobroQrService.confirmarSiHay(COBRO_QR_ID, NEGOCIO_ID, transferido, AHORA.plusMinutes(5));
    }

    private void cuandoSeIntentaConfirmarPor(BigDecimal transferido) {
        rechazo = catchThrowable(() -> cuandoSeConfirmaPor(transferido));
    }

    // --- entonces ---
    private void entoncesNoSeTocaElRepositorio() {
        verifyNoInteractions(cobroQrRepository);
    }

    private void entoncesSeGuardaElCobroCobradoPorConfirmacionManual() {
        ArgumentCaptor<CobroQr> guardado = ArgumentCaptor.forClass(CobroQr.class);
        verify(cobroQrRepository).save(guardado.capture());
        assertThat(guardado.getValue().estado()).isEqualTo(EstadoCobroQr.COBRADO);
        assertThat(guardado.getValue().confirmacion()).isEqualTo(ConfirmacionDeCobro.MANUAL);
        assertThat(guardado.getValue().confirmadoEn()).isEqualTo(AHORA.plusMinutes(5));
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
