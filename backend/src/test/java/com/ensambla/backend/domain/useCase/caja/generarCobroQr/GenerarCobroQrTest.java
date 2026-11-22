package com.ensambla.backend.domain.useCase.caja.generarCobroQr;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.CobroQr;
import com.ensambla.backend.domain.model.caja.EstadoCobroQr;
import com.ensambla.backend.domain.model.caja.EstadoOrdenCobroQr;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.caja.CobroQrRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.port.PasarelaCobrosQrPort;
import com.ensambla.backend.domain.useCase.caja.CobroQrItem;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.CobrosQrDePrueba.unaOrdenQr;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioConDestinoDeCobro;
import static com.ensambla.backend.support.NegociosDePrueba.unNegocioSinUbicacion;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class GenerarCobroQrTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final CobroQrRepository cobroQrRepository = mock(CobroQrRepository.class);
    private final PasarelaCobrosQrPort pasarelaCobrosQrPort = mock(PasarelaCobrosQrPort.class);
    private final GenerarCobroQr generarCobroQr = new GenerarCobroQr(negocioRepository, cobroQrRepository,
            pasarelaCobrosQrPort, new TransaccionDirecta(), Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private CobroQrItem respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Genera el cobro pendiente con el alias del negocio y lo guarda")
    void generaElCobroPendienteConElAliasDelNegocioYLoGuarda() {
        dadoUnNegocioQueCobraEnSuAlias();
        dadoQueNoHayUnCobroPrevio();

        cuandoSeGeneraElCobroPorElImporte(IMPORTE);

        entoncesSeGuardaUnCobroPendienteConElDestinoDelNegocio();
        entoncesSeResponde("PENDIENTE");
    }

    @Test
    @DisplayName("Repetir el mismo cobro devuelve el que ya estaba sin guardar otro")
    void repetirElMismoCobroDevuelveElQueYaEstabaSinGuardarOtro() {
        dadoUnNegocioQueCobraEnSuAlias();
        dadoUnCobroPrevio();

        cuandoSeGeneraElCobroPorElImporte(IMPORTE);

        entoncesSeResponde("PENDIENTE");
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Reutilizar el id de un cobro con otro importe responde conflicto")
    void reutilizarElIdDeUnCobroConOtroImporteRespondeConflicto() {
        dadoUnNegocioQueCobraEnSuAlias();
        dadoUnCobroPrevio();

        cuandoSeIntentaGenerarElCobroPorElImporte(IMPORTE.add(BigDecimal.ONE));

        entoncesSeRechazaCon(DuplicateResourceException.class);
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("No genera el cobro si el negocio no cargó su alias o CVU")
    void noGeneraElCobroSiElNegocioNoCargoSuAliasOCvu() {
        dadoUnNegocioSinAlias();
        dadoQueNoHayUnCobroPrevio();

        cuandoSeIntentaGenerarElCobroPorElImporte(IMPORTE);

        entoncesSeRechazaCon(BusinessRuleException.class);
        entoncesNoSeGuardaNada();
    }

    @Test
    @DisplayName("Genera una order híbrida de Mercado Pago aunque el negocio no tenga alias")
    void generaUnaOrderHibridaDeMercadoPagoAunqueElNegocioNoTengaAlias() {
        dadoUnNegocioSinAlias();
        dadoQueNoHayUnCobroPrevio();
        dadoMercadoPagoDisponible();

        cuandoSeGeneraElCobroPorElImporte(IMPORTE);

        entoncesSeGuardaElCobroDeMercadoPago();
    }

    @Test
    @DisplayName("No solicita una order de Mercado Pago por menos de quince pesos")
    void noSolicitaUnaOrderDeMercadoPagoPorMenosDeQuincePesos() {
        dadoUnNegocioSinAlias();
        dadoQueNoHayUnCobroPrevio();
        dadoMercadoPagoDisponible();

        cuandoSeIntentaGenerarElCobroPorElImporte(new BigDecimal("14.99"));

        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Mercado Pago permite cobrar con QR desde $15");
        verify(pasarelaCobrosQrPort, never()).crear(any(), any());
        entoncesNoSeGuardaNada();
    }

    // --- dado ---
    private void dadoUnNegocioQueCobraEnSuAlias() {
        dadoUnNegocio(unNegocioConDestinoDeCobro("mi.alias.mp", "Ana Pérez"));
    }

    private void dadoUnNegocioSinAlias() {
        dadoUnNegocio(unNegocioSinUbicacion());
    }

    private void dadoUnNegocio(Negocio negocio) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
    }

    private void dadoQueNoHayUnCobroPrevio() {
        when(cobroQrRepository.findByIdAndNegocioId(COBRO_QR_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
        when(cobroQrRepository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private void dadoUnCobroPrevio() {
        when(cobroQrRepository.findByIdAndNegocioId(COBRO_QR_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unCobroQrPendiente()));
    }

    private void dadoMercadoPagoDisponible() {
        when(pasarelaCobrosQrPort.disponible()).thenReturn(true);
        when(pasarelaCobrosQrPort.crear(COBRO_QR_ID, IMPORTE))
                .thenReturn(unaOrdenQr(EstadoOrdenCobroQr.PENDIENTE));
    }

    // --- cuando ---
    private void cuandoSeGeneraElCobroPorElImporte(BigDecimal importe) {
        respuesta = generarCobroQr.execute(new GenerarCobroQrRequest(USUARIO_ID, COBRO_QR_ID, importe));
    }

    private void cuandoSeIntentaGenerarElCobroPorElImporte(BigDecimal importe) {
        rechazo = catchThrowable(() -> cuandoSeGeneraElCobroPorElImporte(importe));
    }

    // --- entonces ---
    private void entoncesSeGuardaUnCobroPendienteConElDestinoDelNegocio() {
        ArgumentCaptor<CobroQr> guardado = ArgumentCaptor.forClass(CobroQr.class);
        verify(cobroQrRepository).save(guardado.capture());
        assertThat(guardado.getValue().id()).isEqualTo(COBRO_QR_ID);
        assertThat(guardado.getValue().negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getValue().estado()).isEqualTo(EstadoCobroQr.PENDIENTE);
        assertThat(guardado.getValue().destino().alias()).isEqualTo("mi.alias.mp");
        assertThat(guardado.getValue().importe()).isEqualByComparingTo(IMPORTE);
    }

    private void entoncesSeResponde(String estado) {
        assertThat(respuesta.id()).isEqualTo(COBRO_QR_ID);
        assertThat(respuesta.estado()).isEqualTo(estado);
        assertThat(respuesta.alias()).isEqualTo("mi.alias.mp");
    }

    private void entoncesSeGuardaElCobroDeMercadoPago() {
        ArgumentCaptor<CobroQr> guardado = ArgumentCaptor.forClass(CobroQr.class);
        verify(cobroQrRepository).save(guardado.capture());
        assertThat(guardado.getValue().usaMercadoPago()).isTrue();
        assertThat(guardado.getValue().qrDinamico()).isEqualTo("qr-dinamico");
        assertThat(respuesta.proveedor()).isEqualTo("MERCADO_PAGO");
    }

    private void entoncesSeRechazaCon(Class<? extends Throwable> tipo) {
        assertThat(rechazo).isInstanceOf(tipo);
    }

    private void entoncesNoSeGuardaNada() {
        verify(cobroQrRepository, never()).save(any());
    }
}
