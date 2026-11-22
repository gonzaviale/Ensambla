package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;

import static com.ensambla.backend.support.CobrosQrDePrueba.AHORA;
import static com.ensambla.backend.support.CobrosQrDePrueba.COBRO_QR_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.IMPORTE;
import static com.ensambla.backend.support.CobrosQrDePrueba.ORDEN_ID;
import static com.ensambla.backend.support.CobrosQrDePrueba.unCobroQrMercadoPago;
import static com.ensambla.backend.support.CobrosQrDePrueba.unaOrdenQr;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CobroQrMercadoPagoTest {

    private CobroQr resultado;
    private Throwable rechazo;

    @ParameterizedTest
    @EnumSource(EstadoOrdenCobroQr.class)
    @DisplayName("Cada estado de la order se refleja en el cobro con QR")
    void cadaEstadoDeLaOrderSeReflejaEnElCobroConQr(EstadoOrdenCobroQr estado) {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeSincroniza(unaOrdenQr(estado));
        entoncesQuedaEn(estado.estadoDelCobro());
    }

    @Test
    @DisplayName("El pago acreditado se confirma por Mercado Pago una sola vez")
    void elPagoAcreditadoSeConfirmaPorMercadoPagoUnaSolaVez() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeAcreditaDosVeces();
        entoncesConservaLaPrimeraAcreditacion();
    }

    @Test
    @DisplayName("Una consulta atrasada no hace retroceder un pago acreditado")
    void unaConsultaAtrasadaNoHaceRetrocederUnPagoAcreditado() {
        dadoUnCobroAcreditadoDeMercadoPago();

        cuandoSeSincroniza(unaOrdenQr(EstadoOrdenCobroQr.PENDIENTE));

        entoncesQuedaEn(EstadoCobroQr.COBRADO);
    }

    @Test
    @DisplayName("Un pago acreditado puede pasar a reembolsado")
    void unPagoAcreditadoPuedePasarAReembolsado() {
        dadoUnCobroAcreditadoDeMercadoPago();

        cuandoSeSincroniza(unaOrdenQr(EstadoOrdenCobroQr.REEMBOLSADO));

        entoncesQuedaEn(EstadoCobroQr.REEMBOLSADO);
    }

    @ParameterizedTest
    @EnumSource(value = EstadoOrdenCobroQr.class, names = {"VENCIDO", "CANCELADO", "REEMBOLSADO"})
    @DisplayName("Una respuesta atrasada no hace retroceder un cobro finalizado")
    void unaRespuestaAtrasadaNoHaceRetrocederUnCobroFinalizado(EstadoOrdenCobroQr estadoFinal) {
        resultado = unCobroQrMercadoPago(estadoFinal);

        cuandoSeSincroniza(unaOrdenQr(EstadoOrdenCobroQr.PENDIENTE));

        entoncesQuedaEn(estadoFinal.estadoDelCobro());
    }

    @Test
    @DisplayName("Una actualización sin tramas conserva los dos QR originales")
    void unaActualizacionSinTramasConservaLosDosQrOriginales() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeSincroniza(ordenSinTramas());
        entoncesConservaLosQr();
    }

    @Test
    @DisplayName("Una actualización con QR nuevos reemplaza las tramas anteriores")
    void unaActualizacionConQrNuevosReemplazaLasTramasAnteriores() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeSincroniza(ordenConQrNuevos());
        entoncesUsaLosQrNuevos();
    }

    @Test
    @DisplayName("Una order con otra referencia se rechaza")
    void unaOrderConOtraReferenciaSeRechaza() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeIntentaSincronizar(ordenConOtraReferencia());
        entoncesSeRechazaPorNoCoincidir();
    }

    @Test
    @DisplayName("Una order con otro importe se rechaza")
    void unaOrderConOtroImporteSeRechaza() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeIntentaSincronizar(ordenConOtroImporte());
        entoncesSeRechazaPorNoCoincidir();
    }

    @Test
    @DisplayName("Un cobro pendiente de Mercado Pago no se puede registrar como cobrado")
    void unCobroPendienteDeMercadoPagoNoSePuedeRegistrarComoCobrado() {
        dadoUnCobroPendienteDeMercadoPago();
        cuandoSeExigeQueEsteAcreditado();
        entoncesSeRechazaPorqueTodaviaNoSeAcredito();
    }

    @Test
    @DisplayName("Un cobro acreditado de Mercado Pago se puede registrar")
    void unCobroAcreditadoDeMercadoPagoSePuedeRegistrar() {
        dadoUnCobroAcreditadoDeMercadoPago();
        cuandoSeExigeQueEsteAcreditado();
        entoncesNoSeRechaza();
    }

    private void dadoUnCobroPendienteDeMercadoPago() {
        resultado = unCobroQrMercadoPago(EstadoOrdenCobroQr.PENDIENTE);
    }

    private void dadoUnCobroAcreditadoDeMercadoPago() {
        resultado = unCobroQrMercadoPago(EstadoOrdenCobroQr.COBRADO);
    }

    private void cuandoSeSincroniza(OrdenCobroQr orden) {
        resultado = resultado.sincronizar(orden, AHORA.plusMinutes(2));
    }

    private void cuandoSeAcreditaDosVeces() {
        resultado = resultado.sincronizar(unaOrdenQr(EstadoOrdenCobroQr.COBRADO), AHORA.plusMinutes(2));
        resultado = resultado.sincronizar(unaOrdenQr(EstadoOrdenCobroQr.COBRADO), AHORA.plusMinutes(3));
    }

    private void cuandoSeIntentaSincronizar(OrdenCobroQr orden) {
        rechazo = catchThrowable(() -> resultado.sincronizar(orden, AHORA.plusMinutes(2)));
    }

    private void cuandoSeExigeQueEsteAcreditado() {
        rechazo = catchThrowable(resultado::exigirAcreditadoPorMercadoPago);
    }

    private OrdenCobroQr ordenSinTramas() {
        return new OrdenCobroQr(ORDEN_ID, COBRO_QR_ID, IMPORTE, EstadoOrdenCobroQr.PENDIENTE,
                "created", "PAY01ENSAMBLA", null, " ");
    }

    private OrdenCobroQr ordenConQrNuevos() {
        return new OrdenCobroQr(ORDEN_ID, COBRO_QR_ID, IMPORTE, EstadoOrdenCobroQr.PENDIENTE,
                "created", "PAY01ENSAMBLA", "qr-dinamico-nuevo", "qr-estatico-nuevo");
    }

    private OrdenCobroQr ordenConOtraReferencia() {
        return new OrdenCobroQr(ORDEN_ID, CobroQrId.nuevo(), IMPORTE, EstadoOrdenCobroQr.PENDIENTE,
                "created", "PAY01ENSAMBLA", null, null);
    }

    private OrdenCobroQr ordenConOtroImporte() {
        return new OrdenCobroQr(ORDEN_ID, COBRO_QR_ID, IMPORTE.add(BigDecimal.ONE),
                EstadoOrdenCobroQr.PENDIENTE, "created", "PAY01ENSAMBLA", null, null);
    }

    private void entoncesQuedaEn(EstadoCobroQr estado) {
        assertThat(resultado.estado()).isEqualTo(estado);
        assertThat(resultado.usaMercadoPago()).isTrue();
        assertThat(resultado.proveedorOrdenId()).isEqualTo(ORDEN_ID);
    }

    private void entoncesConservaLaPrimeraAcreditacion() {
        assertThat(resultado.confirmacion()).isEqualTo(ConfirmacionDeCobro.MERCADO_PAGO);
        assertThat(resultado.confirmadoEn()).isEqualTo(AHORA.plusMinutes(2));
    }

    private void entoncesConservaLosQr() {
        assertThat(resultado.qrDinamico()).isEqualTo("qr-dinamico");
        assertThat(resultado.qrEstatico()).isEqualTo("qr-estatico");
    }

    private void entoncesUsaLosQrNuevos() {
        assertThat(resultado.qrDinamico()).isEqualTo("qr-dinamico-nuevo");
        assertThat(resultado.qrEstatico()).isEqualTo("qr-estatico-nuevo");
    }

    private void entoncesSeRechazaPorNoCoincidir() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La orden de Mercado Pago no coincide con el cobro con QR");
    }

    private void entoncesSeRechazaPorqueTodaviaNoSeAcredito() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Mercado Pago todavía no acreditó el cobro con QR");
    }

    private void entoncesNoSeRechaza() {
        assertThat(rechazo).isNull();
        assertThat(resultado.negocioId()).isEqualTo(NEGOCIO_ID);
    }
}
