package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.useCase.caja.CobroCajaItem;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CobroCajaTest {

    private CobroCaja cobro;
    private CobroCajaItem respuesta;
    private VentaCaja original;
    private VentaCaja actual;
    private ResumenCaja resumen;
    private Throwable rechazo;

    @Test
    @DisplayName("Un cobro conserva fecha, cliente, medios y aplicaciones en su respuesta")
    void unCobroConservaFechaClienteMediosYAplicacionesEnSuRespuesta() {
        dadoUnCobroRegistrado();

        cuandoSeArmaLaRespuestaDelCobro();

        entoncesLaRespuestaConservaElDetalleDelCobro();
    }

    @Test
    @DisplayName("Un pago posterior reduce el saldo sin alterar el importe cobrado al vender")
    void unPagoPosteriorReduceElSaldoSinAlterarElImporteCobradoAlVender() {
        dadoUnaVentaConSaldoPendiente();

        cuandoSeCobraPorTransferencia("600");

        entoncesElSaldoBajaYSeConservanLosPagosIniciales();
    }

    @Test
    @DisplayName("Completar una deuda cambia automáticamente el estado a pagada")
    void completarUnaDeudaCambiaAutomaticamenteElEstadoAPagada() {
        dadoUnaVentaFiadaCompleta();

        cuandoSeCobraPorTransferencia("2000");

        entoncesLaVentaQuedaPagadaPorTransferencia();
    }

    @Test
    @DisplayName("Acumula entregas del mismo medio sin duplicar el cobro inicial")
    void acumulaEntregasDelMismoMedioSinDuplicarElCobroInicial() {
        dadoUnaVentaConSaldoPendiente();

        cuandoSeCobraEnEfectivo("600");

        entoncesElEfectivoSeAcumulaEnUnSoloPago();
    }

    @Test
    @DisplayName("Una entrega superior al saldo se rechaza")
    void unaEntregaSuperiorAlSaldoSeRechaza() {
        dadoUnaVentaConSaldoPendiente();

        cuandoSeIntentaCobrarEnEfectivo("1201");

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza un cobro sin ventas aplicadas")
    void rechazaUnCobroSinVentasAplicadas() {
        cuandoSeIntentaCrearUnCobroConAplicaciones(List.of());

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza aplicar dos veces el mismo cobro a una venta")
    void rechazaAplicarDosVecesElMismoCobroAUnaVenta() {
        CobroCaja.Aplicacion aplicacion = CobroCajaTestData.cobro().aplicaciones().getFirst();

        cuandoSeIntentaCrearUnCobroConAplicaciones(List.of(aplicacion, aplicacion));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza una aplicación sin pagos")
    void rechazaUnaAplicacionSinPagos() {
        cuandoSeIntentaCrearUnaAplicacionConPagos(List.of());

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza medios repetidos en la misma aplicación")
    void rechazaMediosRepetidosEnLaMismaAplicacion() {
        PagoCaja pago = new PagoCaja(MedioPagoCaja.EFECTIVO, BigDecimal.ONE);

        cuandoSeIntentaCrearUnaAplicacionConPagos(List.of(pago, pago));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("Rechaza más de tres partes en una aplicación")
    void rechazaMasDeTresPartesEnUnaAplicacion() {
        PagoCaja pago = new PagoCaja(MedioPagoCaja.EFECTIVO, BigDecimal.ONE);

        cuandoSeIntentaCrearUnaAplicacionConPagos(List.of(pago, pago, pago, pago));

        entoncesSeRechazaPorReglaDeNegocio();
    }

    @Test
    @DisplayName("El resumen del día suma sólo el cobro inicial y los movimientos recibidos ese día")
    void elResumenDelDiaSumaSoloElCobroInicialYLosMovimientosRecibidosEseDia() {
        dadoUnaVentaConSaldoPendiente();

        cuandoSeResumeElDiaConUnCobroPosterior();

        entoncesElResumenSeparaElDineroInicialDelPosterior();
    }

    // --- dado ---

    private void dadoUnCobroRegistrado() {
        cobro = CobroCajaTestData.cobro();
    }

    private void dadoUnaVentaConSaldoPendiente() {
        original = CobroCajaTestData.venta();
    }

    private void dadoUnaVentaFiadaCompleta() {
        original = CobroCajaTestData.venta(CajaTestData.OPERACION, "0");
    }

    // --- cuando ---

    private void cuandoSeArmaLaRespuestaDelCobro() {
        respuesta = CobroCajaItem.de(cobro);
    }

    private void cuandoSeCobraPorTransferencia(String importe) {
        actual = original.conCobro(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal(importe))));
    }

    private void cuandoSeCobraEnEfectivo(String importe) {
        actual = original.conCobro(List.of(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal(importe))));
    }

    private void cuandoSeIntentaCobrarEnEfectivo(String importe) {
        rechazo = catchThrowable(() -> cuandoSeCobraEnEfectivo(importe));
    }

    private void cuandoSeIntentaCrearUnCobroConAplicaciones(List<CobroCaja.Aplicacion> aplicaciones) {
        CobroCaja base = CobroCajaTestData.cobro();
        rechazo = catchThrowable(() -> new CobroCaja(base.id(), base.negocioId(), base.proyectoId(),
                base.inventarioId(), base.clienteId(), base.clienteNombre(), base.huella(), aplicaciones,
                base.creadoEn()));
    }

    private void cuandoSeIntentaCrearUnaAplicacionConPagos(List<PagoCaja> pagos) {
        rechazo = catchThrowable(() -> new CobroCaja.Aplicacion(CajaTestData.OPERACION, pagos));
    }

    private void cuandoSeResumeElDiaConUnCobroPosterior() {
        VentaCaja venta = original.conCobro(CobroCajaTestData.cobro().aplicaciones().getFirst().pagos());
        resumen = ResumenCaja.de(List.of(venta), List.of(CobroCajaTestData.cobro()));
    }

    // --- entonces ---

    private void entoncesLaRespuestaConservaElDetalleDelCobro() {
        assertThat(respuesta.id()).isEqualTo(cobro.id());
        assertThat(respuesta.clienteId()).isEqualTo(CajaTestData.CLIENTE);
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana López");
        assertThat(respuesta.creadoEn()).isEqualTo(cobro.creadoEn());
        assertThat(respuesta.importe()).isEqualByComparingTo("600");
        var aplicacion = respuesta.aplicaciones().getFirst();
        assertThat(aplicacion.ventaId()).isEqualTo(CajaTestData.OPERACION);
        assertThat(aplicacion.importe()).isEqualByComparingTo("600");
        assertThat(aplicacion.pagos().getFirst().medio()).isEqualTo("TRANSFERENCIA");
        assertThat(aplicacion.pagos().getFirst().importe()).isEqualByComparingTo("600");
    }

    private void entoncesElSaldoBajaYSeConservanLosPagosIniciales() {
        assertThat(actual.saldo()).isEqualByComparingTo("600");
        assertThat(actual.cobrado()).isEqualByComparingTo("1400");
        assertThat(actual.pagosIniciales()).isEqualTo(original.pagos());
        assertThat(actual.medio()).isEqualTo(MedioPagoCaja.MIXTO);
        assertThat(original.saldo()).isEqualByComparingTo("1200");
    }

    private void entoncesLaVentaQuedaPagadaPorTransferencia() {
        assertThat(actual.estado()).isEqualTo(EstadoVentaCaja.PAGADA);
        assertThat(actual.saldo()).isEqualByComparingTo("0");
        assertThat(actual.medio()).isEqualTo(MedioPagoCaja.TRANSFERENCIA);
    }

    private void entoncesElEfectivoSeAcumulaEnUnSoloPago() {
        assertThat(actual.pagos()).containsExactly(new PagoCaja(MedioPagoCaja.EFECTIVO, new BigDecimal("1400")));
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesElResumenSeparaElDineroInicialDelPosterior() {
        assertThat(resumen.total()).isEqualByComparingTo("2000");
        assertThat(resumen.efectivo()).isEqualByComparingTo("800");
        assertThat(resumen.transferencia()).isEqualByComparingTo("600");
        assertThat(resumen.tarjeta()).isEqualByComparingTo("0");
        assertThat(resumen.desde()).isEqualTo(CajaTestData.AHORA);
    }
}
