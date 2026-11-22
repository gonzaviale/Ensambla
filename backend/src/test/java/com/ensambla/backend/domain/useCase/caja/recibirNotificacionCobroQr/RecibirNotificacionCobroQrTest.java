package com.ensambla.backend.domain.useCase.caja.recibirNotificacionCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.ORDEN_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrMercadoPago;
import static com.ensambla.backend.support.CobrosQrDePrueba.unaOrdenQr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecibirNotificacionCobroQrTest {

    private final PasarelaCobrosQrPort pasarelaCobrosQrPort = mock(PasarelaCobrosQrPort.class);
    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class);
    private final RecibirNotificacionCobroQr recibir = new RecibirNotificacionCobroQr(pasarelaCobrosQrPort,
            cobroQrRepository, new TransaccionDirecta(), Clock.fixed(AHORA.plusMinutes(2).toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Una notificación de order firmada acredita el cobro encontrado")
    void unaNotificacionDeOrderFirmadaAcreditaElCobroEncontrado() {
        dadoUnaOrderAcreditadaConCobro();
        cuandoSeRecibe("order");
        entoncesSeValidaYGuardaCobrado();
    }

    @Test
    @DisplayName("Una order desconocida se valida y concilia sin guardar otro cobro")
    void unaOrderDesconocidaSeValidaYConciliaSinGuardarOtroCobro() {
        dadoUnaOrderAcreditadaSinCobro();
        cuandoSeRecibe("order");
        entoncesNoSeGuardaNingunCobro();
    }

    @Test
    @DisplayName("Otro tipo de notificación se valida pero no se procesa como order")
    void otroTipoDeNotificacionSeValidaPeroNoSeProcesaComoOrder() {
        cuandoSeRecibe("payment");
        entoncesSoloSeValidaLaFirma();
    }

    private void dadoUnaOrderAcreditadaConCobro() {
        dadoUnaOrderAcreditadaSinCobro();
        when(cobroQrRepository.findByProveedorOrdenId(ORDEN_ID))
                .thenReturn(Optional.of(unCobroQrMercadoPago(EstadoOrdenCobroQr.PENDIENTE)));
        when(cobroQrRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnaOrderAcreditadaSinCobro() {
        when(pasarelaCobrosQrPort.consultar(ORDEN_ID)).thenReturn(unaOrdenQr(EstadoOrdenCobroQr.COBRADO));
        when(cobroQrRepository.findByProveedorOrdenId(ORDEN_ID)).thenReturn(Optional.empty());
    }

    private void cuandoSeRecibe(String tipo) {
        recibir.execute(tipo, ORDEN_ID, "firma", "solicitud");
    }

    private void entoncesSeValidaYGuardaCobrado() {
        verify(pasarelaCobrosQrPort).validarNotificacion("firma", "solicitud", ORDEN_ID);
        var captor = org.mockito.ArgumentCaptor.forClass(CobroQr.class);
        verify(cobroQrRepository).save(captor.capture());
        assertThat(captor.getValue().estado()).isEqualTo(com.ensambla.backend.domain.model.caja.EstadoCobroQr.COBRADO);
    }

    private void entoncesNoSeGuardaNingunCobro() {
        verify(cobroQrRepository, never()).save(any());
    }

    private void entoncesSoloSeValidaLaFirma() {
        verify(pasarelaCobrosQrPort).validarNotificacion("firma", "solicitud", ORDEN_ID);
        verify(pasarelaCobrosQrPort, never()).consultar(any());
        verify(cobroQrRepository, never()).findByProveedorOrdenId(any());
    }
}
