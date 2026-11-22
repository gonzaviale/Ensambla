package com.ensambla.backend.domain.useCase.caja.consultarCobroQr;

import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.ORDEN_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrMercadoPago;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.CobrosQrDePrueba.unaOrdenQr;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarCobroQrTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class);
    private final PasarelaCobrosQrPort pasarelaCobrosQrPort = mock(PasarelaCobrosQrPort.class);
    private final ConsultarCobroQr consultarCobroQr = new ConsultarCobroQr(negocioRepository, cobroQrRepository,
            pasarelaCobrosQrPort, new TransaccionDirecta(), Clock.fixed(AHORA.plusMinutes(2).toInstant(), ZoneOffset.UTC));

    private CobroQrItem respuesta;

    @Test
    @DisplayName("Concilia una order pendiente y guarda el pago acreditado")
    void conciliaUnaOrderPendienteYGuardaElPagoAcreditado() {
        dadoElCobro(unCobroQrMercadoPago(EstadoOrdenCobroQr.PENDIENTE));
        dadoQueMercadoPagoLoAcredito();
        cuandoSeConsultaElCobro();
        entoncesSeGuardaYRespondeCobrado();
    }

    @Test
    @DisplayName("Un cobro manual se consulta sin llamar a Mercado Pago")
    void unCobroManualSeConsultaSinLlamarAMercadoPago() {
        dadoElCobro(unCobroQrPendiente());
        cuandoSeConsultaElCobro();
        entoncesSeRespondePendienteSinConciliar();
    }

    @Test
    @DisplayName("Un cobro finalizado se consulta sin volver a llamar a Mercado Pago")
    void unCobroFinalizadoSeConsultaSinVolverALlamarAMercadoPago() {
        dadoElCobro(unCobroQrMercadoPago(EstadoOrdenCobroQr.COBRADO));
        cuandoSeConsultaElCobro();
        entoncesSeRespondeCobradoSinConciliar();
    }

    private void dadoElCobro(CobroQr cobro) {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(cobroQrRepository.obtener(COBRO_QR_ID, NEGOCIO_ID)).thenReturn(cobro);
    }

    private void dadoQueMercadoPagoLoAcredito() {
        when(pasarelaCobrosQrPort.consultar(ORDEN_ID)).thenReturn(unaOrdenQr(EstadoOrdenCobroQr.COBRADO));
        when(cobroQrRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void cuandoSeConsultaElCobro() {
        respuesta = consultarCobroQr.execute(new ConsultarCobroQrRequest(USUARIO_ID, COBRO_QR_ID));
    }

    private void entoncesSeGuardaYRespondeCobrado() {
        assertThat(respuesta.estado()).isEqualTo("COBRADO");
        verify(cobroQrRepository).save(any());
    }

    private void entoncesSeRespondePendienteSinConciliar() {
        assertThat(respuesta.estado()).isEqualTo("PENDIENTE");
        verifyNoInteractions(pasarelaCobrosQrPort);
        verify(cobroQrRepository, never()).save(any());
    }

    private void entoncesSeRespondeCobradoSinConciliar() {
        assertThat(respuesta.estado()).isEqualTo("COBRADO");
        verifyNoInteractions(pasarelaCobrosQrPort);
        verify(cobroQrRepository, never()).save(any());
    }
}
