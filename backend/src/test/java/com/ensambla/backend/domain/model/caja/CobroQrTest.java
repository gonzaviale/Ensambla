package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.negocio.DestinoDeCobro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.elMomentoDelVencimiento;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCancelado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrCobrado;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrPendiente;
import static com.ensambla.backend.support.CobrosQrDePrueba.unDestinoDeCobro;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CobroQrTest {

    private CobroQr cobro;
    private CobroQr resultado;
    private EstadoCobroQr estadoVisto;
    private Throwable rechazo;

    @Test
    @DisplayName("Un cobro nuevo queda pendiente con su importe y su destino, y vence a los 15 minutos")
    void unCobroNuevoQuedaPendienteConSuImporteYSuDestinoYVenceALos15Minutos() {
        cuandoSeCreaUnCobroNuevo();

        entoncesQuedaPendienteConSuImporteYSuDestino();
        entoncesVenceALos15Minutos();
        entoncesNoTieneConfirmacion();
    }

    @Test
    @DisplayName("No se crea un cobro si el negocio no cargó su alias o CVU")
    void noSeCreaUnCobroSiElNegocioNoCargoSuAliasOCvu() {
        cuandoSeIntentaCrearUnCobroSinDestino();

        entoncesSeRechazaPorque("Cargá tu alias o CVU en Mi negocio para cobrar con QR");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-10", "10.005"})
    @DisplayName("No se crea un cobro con un importe que no sea mayor a cero con hasta dos decimales")
    void noSeCreaUnCobroConUnImporteQueNoSeaMayorACeroConHastaDosDecimales(String importe) {
        cuandoSeIntentaCrearUnCobroPor(new BigDecimal(importe));

        entoncesSeRechazaPorque("El importe del cobro con QR debe ser mayor a cero con hasta dos decimales");
    }

    @Test
    @DisplayName("Un cobro pendiente se ve pendiente antes de su vencimiento")
    void unCobroPendienteSeVePendienteAntesDeSuVencimiento() {
        dadoUnCobroPendiente();

        cuandoSeMiraElEstadoEn(elMomentoDelVencimiento().minusSeconds(1));

        entoncesSeVe(EstadoCobroQr.PENDIENTE);
    }

    @Test
    @DisplayName("Un cobro pendiente se ve vencido al llegar su vencimiento")
    void unCobroPendienteSeVeVencidoAlLlegarSuVencimiento() {
        dadoUnCobroPendiente();

        cuandoSeMiraElEstadoEn(elMomentoDelVencimiento());

        entoncesSeVe(EstadoCobroQr.VENCIDO);
    }

    @Test
    @DisplayName("Un cobro cobrado se sigue viendo cobrado después de su vencimiento")
    void unCobroCobradoSeSigueViendoCobradoDespuesDeSuVencimiento() {
        dadoUnCobroCobrado();

        cuandoSeMiraElEstadoEn(elMomentoDelVencimiento().plusHours(1));

        entoncesSeVe(EstadoCobroQr.COBRADO);
    }

    @Test
    @DisplayName("Confirmar a mano deja el cobro cobrado con su fecha y la marca de confirmación manual")
    void confirmarAManoDejaElCobroCobradoConSuFechaYLaMarcaDeConfirmacionManual() {
        dadoUnCobroPendiente();

        cuandoSeConfirmaAManoEn(AHORA.plusMinutes(5));

        entoncesQuedaCobradoPorConfirmacionManualEn(AHORA.plusMinutes(5));
        entoncesConservaElImporteElDestinoYLaFechaDeCreacion();
    }

    @Test
    @DisplayName("Un cobro vencido todavía se puede confirmar para no perder una venta que sí se pagó")
    void unCobroVencidoTodaviaSePuedeConfirmarParaNoPerderUnaVentaQueSiSePago() {
        dadoUnCobroPendiente();

        cuandoSeConfirmaAManoEn(elMomentoDelVencimiento().plusMinutes(30));

        entoncesQuedaCobradoPorConfirmacionManualEn(elMomentoDelVencimiento().plusMinutes(30));
    }

    @Test
    @DisplayName("Un cobro cancelado no se puede confirmar")
    void unCobroCanceladoNoSePuedeConfirmar() {
        dadoUnCobroCancelado();

        cuandoSeIntentaConfirmarAMano();

        entoncesSeRechazaPorque("El cobro con QR está cancelado");
    }

    @Test
    @DisplayName("Un cobro ya cobrado no se confirma dos veces")
    void unCobroYaCobradoNoSeConfirmaDosVeces() {
        dadoUnCobroCobrado();

        cuandoSeIntentaConfirmarAMano();

        entoncesSeRechazaPorque("El cobro con QR ya está cobrado");
    }

    @Test
    @DisplayName("Cancelar un cobro pendiente lo deja cancelado y sin confirmación")
    void cancelarUnCobroPendienteLoDejaCanceladoYSinConfirmacion() {
        dadoUnCobroPendiente();

        cuandoSeCancelaEn(AHORA.plusMinutes(1));

        entoncesQuedaCanceladoSinConfirmacionEn(AHORA.plusMinutes(1));
    }

    @Test
    @DisplayName("Un cobro vencido se puede cancelar")
    void unCobroVencidoSePuedeCancelar() {
        dadoUnCobroPendiente();

        cuandoSeCancelaEn(elMomentoDelVencimiento().plusMinutes(1));

        entoncesQuedaCanceladoSinConfirmacionEn(elMomentoDelVencimiento().plusMinutes(1));
    }

    @Test
    @DisplayName("Un cobro ya cobrado no se puede cancelar")
    void unCobroYaCobradoNoSePuedeCancelar() {
        dadoUnCobroCobrado();

        cuandoSeIntentaCancelar();

        entoncesSeRechazaPorque("El cobro con QR ya está cobrado");
    }

    @Test
    @DisplayName("Acepta lo transferido si coincide con el importe aunque tenga otra escala")
    void aceptaLoTransferidoSiCoincideConElImporteAunqueTengaOtraEscala() {
        dadoUnCobroPendiente();

        cuandoSeExigeLaTransferenciaPor(new BigDecimal("2500.500"));

        entoncesNoSeRechaza();
    }

    @Test
    @DisplayName("Rechaza lo transferido si no coincide con el importe")
    void rechazaLoTransferidoSiNoCoincideConElImporte() {
        dadoUnCobroPendiente();

        cuandoSeExigeLaTransferenciaPor(new BigDecimal("2500.49"));

        entoncesSeRechazaPorque("El importe transferido no coincide con el del cobro con QR");
    }

    // --- dado ---
    private void dadoUnCobroPendiente() {
        cobro = unCobroQrPendiente();
    }

    private void dadoUnCobroCobrado() {
        cobro = unCobroQrCobrado();
    }

    private void dadoUnCobroCancelado() {
        cobro = unCobroQrCancelado();
    }

    // --- cuando ---
    private void cuandoSeCreaUnCobroNuevo() {
        cobro = unCobroQrPendiente();
    }

    private void cuandoSeIntentaCrearUnCobroSinDestino() {
        rechazo = catchThrowable(() -> CobroQr.nuevo(COBRO_QR_ID, NEGOCIO_ID, DestinoDeCobro.SIN_CARGAR, IMPORTE, AHORA));
    }

    private void cuandoSeIntentaCrearUnCobroPor(BigDecimal importe) {
        rechazo = catchThrowable(() -> CobroQr.nuevo(COBRO_QR_ID, NEGOCIO_ID, unDestinoDeCobro(), importe, AHORA));
    }

    private void cuandoSeExigeLaTransferenciaPor(BigDecimal transferido) {
        rechazo = catchThrowable(() -> cobro.exigirTransferenciaPor(transferido));
    }

    private void cuandoSeMiraElEstadoEn(OffsetDateTime momento) {
        estadoVisto = cobro.estadoEn(momento);
    }

    private void cuandoSeConfirmaAManoEn(OffsetDateTime momento) {
        resultado = cobro.confirmarManualmente(momento);
    }

    private void cuandoSeIntentaConfirmarAMano() {
        rechazo = catchThrowable(() -> cobro.confirmarManualmente(AHORA.plusMinutes(10)));
    }

    private void cuandoSeCancelaEn(OffsetDateTime momento) {
        resultado = cobro.cancelar(momento);
    }

    private void cuandoSeIntentaCancelar() {
        rechazo = catchThrowable(() -> cobro.cancelar(AHORA.plusMinutes(10)));
    }

    // --- entonces ---
    private void entoncesQuedaPendienteConSuImporteYSuDestino() {
        assertThat(cobro.id()).isEqualTo(COBRO_QR_ID);
        assertThat(cobro.negocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(cobro.importe()).isEqualByComparingTo(IMPORTE);
        assertThat(cobro.destino()).isEqualTo(unDestinoDeCobro());
        assertThat(cobro.estado()).isEqualTo(EstadoCobroQr.PENDIENTE);
    }

    private void entoncesVenceALos15Minutos() {
        assertThat(cobro.creadoEn()).isEqualTo(AHORA);
        assertThat(cobro.actualizadoEn()).isEqualTo(AHORA);
        assertThat(cobro.venceEn()).isEqualTo(AHORA.plusMinutes(15));
    }

    private void entoncesNoTieneConfirmacion() {
        assertThat(cobro.confirmadoEn()).isNull();
        assertThat(cobro.confirmacion()).isNull();
    }

    private void entoncesNoSeRechaza() {
        assertThat(rechazo).isNull();
    }

    private void entoncesSeVe(EstadoCobroQr esperado) {
        assertThat(estadoVisto).isEqualTo(esperado);
    }

    private void entoncesQuedaCobradoPorConfirmacionManualEn(OffsetDateTime momento) {
        assertThat(resultado.estado()).isEqualTo(EstadoCobroQr.COBRADO);
        assertThat(resultado.confirmacion()).isEqualTo(ConfirmacionDeCobro.MANUAL);
        assertThat(resultado.confirmadoEn()).isEqualTo(momento);
        assertThat(resultado.actualizadoEn()).isEqualTo(momento);
    }

    private void entoncesConservaElImporteElDestinoYLaFechaDeCreacion() {
        assertThat(resultado.id()).isEqualTo(cobro.id());
        assertThat(resultado.importe()).isEqualByComparingTo(cobro.importe());
        assertThat(resultado.destino()).isEqualTo(cobro.destino());
        assertThat(resultado.creadoEn()).isEqualTo(cobro.creadoEn());
        assertThat(resultado.venceEn()).isEqualTo(cobro.venceEn());
    }

    private void entoncesQuedaCanceladoSinConfirmacionEn(OffsetDateTime momento) {
        assertThat(resultado.estado()).isEqualTo(EstadoCobroQr.CANCELADO);
        assertThat(resultado.confirmadoEn()).isNull();
        assertThat(resultado.confirmacion()).isNull();
        assertThat(resultado.actualizadoEn()).isEqualTo(momento);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }
}
