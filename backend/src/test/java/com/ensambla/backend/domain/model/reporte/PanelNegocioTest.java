package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.reporte.PanelNegocio.SemanaDeCaja;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ensambla.backend.support.DatosDeResumenDePrueba.ACEITE;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.FIDEOS;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.YERBA;
import static com.ensambla.backend.support.DatosDeResumenDePrueba.unaVenta;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.DESCONOCIDO;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.fideos;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenCompleto;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenConEntradas;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenConGastos;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenConGastosYEntradas;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenQueVendio;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unAlmacenSinTablas;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unGasto;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.unaEntrada;
import static com.ensambla.backend.support.DatosDelPanelDePrueba.yerba;
import static org.assertj.core.api.Assertions.assertThat;

class PanelNegocioTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final PeriodoResumen PERIODO = PeriodoResumen.armar(null, null, HOY);
    private static final Integer SIN_PRECIO = null;

    private DatosDeProyecto datos;
    private PeriodoResumen periodo = PERIODO;
    private PanelNegocio panel;

    // --- métricas ---

    @Test
    @DisplayName("Las métricas de ventas, gastos y fiado coinciden con las del resumen del mismo período")
    void lasMetricasCoincidenConLasDelResumenDelMismoPeriodo() {
        dadoUnAlmacen(unAlmacenCompleto());

        cuandoSeArmaElPanel();

        entoncesElPanelEsDelPeriodo(PERIODO);
        entoncesLasMetricasCoincidenConElResumen();
    }

    @Test
    @DisplayName("Calcula lo vendido, lo comprado, los gastos y lo que le deben")
    void calculaLoVendidoLoCompradoLosGastosYLoQueLeDeben() {
        dadoUnAlmacen(unAlmacenCompleto());

        cuandoSeArmaElPanel();

        entoncesVendioComproYGasto("54400", "13500", "6500");
        entoncesLeDeben("17000");
    }

    @Test
    @DisplayName("Calcula lo que queda, el margen, el ticket promedio y la variación")
    void calculaLoQueQuedaElMargenElTicketYLaVariacion() {
        dadoUnAlmacen(unAlmacenCompleto());

        cuandoSeArmaElPanel();

        entoncesLeQueda("17400");
        entoncesElMargenYElTicketPromedioSon("20.6", "18133.33");
        entoncesLaVariacionEs("220");
    }

    @Test
    @DisplayName("Sin tabla de gastos ni de entradas esos datos quedan sin informar, no en cero")
    void sinTablasDeGastosNiEntradasEsosDatosQuedanSinInformar() {
        dadoUnAlmacen(unAlmacenQueVendio(yerba(), unaVenta(YERBA, 10, 3400, "2026-09-22")));

        cuandoSeArmaElPanel();

        entoncesNoSeSabenLosGastosNiLaMercaderia();
        entoncesLeQueda("34000");
        entoncesNoHayVariacion();
    }

    @Test
    @DisplayName("Un gasto sin importe, sin fecha o de otro período no suma")
    void unGastoQueNoSePuedeContarNoSuma() {
        dadoUnAlmacen(unAlmacenConGastos(List.of(
                unGasto("2026-09-23", 5000), unGasto("2026-09-23", null), unGasto("2026-08-01", 9999),
                unGasto(null, 700))));

        cuandoSeArmaElPanel();

        entoncesGasto("5000");
        entoncesLaPrimeraSemanaSalieron("5000");
    }

    @Test
    @DisplayName("Sin costo en los productos vendidos no hay margen")
    void sinCostoEnLosProductosVendidosNoHayMargen() {
        dadoUnAlmacen(unAlmacenQueVendio(fideos(), unaVenta(FIDEOS, 2, 1700, "2026-10-12")));

        cuandoSeArmaElPanel();

        entoncesNoHayMargen();
        entoncesElTicketPromedioEs("3400");
    }

    // --- flujo de caja ---

    @Test
    @DisplayName("Reparte entradas y salidas por semana desde el primer día, con el saldo acumulado")
    void reparteElFlujoDeCajaPorSemana() {
        dadoUnAlmacen(unAlmacenCompleto());

        cuandoSeArmaElPanel();

        entoncesElFlujoTieneSemanas(5);
        entoncesLaSemana(0, "2026-09-20", "2026-09-26", "34000", "18500", "15500", "15500");
        entoncesLaSemana(1, "2026-09-27", "2026-10-03", "0", "0", "0", "15500");
        entoncesLaSemana(2, "2026-10-04", "2026-10-10", "0", "0", "0", "15500");
        entoncesLaSemana(3, "2026-10-11", "2026-10-17", "3400", "1500", "1900", "17400");
        entoncesLaSemana(4, "2026-10-18", "2026-10-19", "0", "0", "0", "17400");
    }

    @Test
    @DisplayName("Lo vendido a fiado no es una entrada hasta cobrarse: se informa aparte")
    void loVendidoAFiadoNoEsUnaEntradaHastaCobrarse() {
        dadoUnAlmacen(unAlmacenCompleto());

        cuandoSeArmaElPanel();

        entoncesEntroEnTotal("37400");
        entoncesVendioComproYGasto("54400", "13500", "6500");
        entoncesLeDeben("17000");
    }

    @Test
    @DisplayName("Un período de una semana exacta tiene una sola semana")
    void unPeriodoDeUnaSemanaExactaTieneUnaSolaSemana() {
        dadoUnAlmacen(unAlmacenCompleto());
        dadoElPeriodo(PeriodoResumen.ultimosDias(7, HOY));

        cuandoSeArmaElPanel();

        entoncesElFlujoTieneSemanas(1);
        entoncesLaSemana(0, "2026-10-13", "2026-10-19", "0", "0", "0", "0");
    }

    @Test
    @DisplayName("Una compra sin producto, cantidad, fecha o costo conocido no cuenta como mercadería")
    void unaCompraQueNoSePuedeValuarNoCuentaComoMercaderia() {
        dadoUnAlmacen(unAlmacenConEntradas(List.of(
                unaEntrada(YERBA.toString(), 5, "2026-09-24"),
                unaEntrada(DESCONOCIDO.toString(), 5, "2026-09-24"),
                unaEntrada(FIDEOS.toString(), 5, "2026-09-24"),
                unaEntrada(ACEITE.toString(), 5, "2026-09-24"),
                unaEntrada(YERBA.toString(), null, "2026-09-24"),
                unaEntrada(YERBA.toString(), 0, "2026-09-24"),
                unaEntrada(YERBA.toString(), 1, "2026-08-01"),
                unaEntrada(YERBA.toString(), 1, "ayer"),
                unaEntrada(YERBA.toString(), 1, null),
                unaEntrada("no-es-un-id", 1, "2026-09-24"),
                unaEntrada(null, 1, "2026-09-24"))));

        cuandoSeArmaElPanel();

        entoncesComproMercaderiaPor("13500");
    }

    // --- mercadería anotada como gasto ---

    @Test
    @DisplayName("La categoría Mercadería no es un gasto ni se suma a las entradas: sería contar la compra dos veces")
    void laMercaderiaAnotadaComoGastoNoSeCuentaDosVeces() {
        dadoUnAlmacen(unAlmacenConGastosYEntradas(List.of(
                unGasto("2026-09-23", 5000),
                unGasto("2026-10-12", 1500),
                unGasto("2026-10-02", 700, "Servicios"),
                unGasto("2026-10-01", 9000, "Mercadería"))));

        cuandoSeArmaElPanel();

        entoncesGasto("7200");
        entoncesComproMercaderiaPor("13500");
        entoncesLeQueda("16700");
    }

    @Test
    @DisplayName("Sin tabla de entradas, la mercadería anotada como gasto es la mercadería y no un gasto más")
    void sinTablaDeEntradasLaMercaderiaSaleDeLosGastos() {
        dadoUnAlmacen(unAlmacenConGastos(List.of(
                unGasto("2026-09-23", 5000),
                unGasto("2026-09-24", 9000, " mercaderia "),
                unGasto("2026-08-01", 700, "Mercadería"))));

        cuandoSeArmaElPanel();

        entoncesGasto("5000");
        entoncesComproMercaderiaPor("9000");
        entoncesLaPrimeraSemanaSalieron("14000");
        entoncesLeQueda("20000");
    }

    @Test
    @DisplayName("Ventas sin precio cuentan como venta pero no hay ticket ni margen que calcular")
    void lasVentasSinPrecioNoTienenTicketNiMargen() {
        dadoUnAlmacen(unAlmacenQueVendio(yerba(), unaVenta(YERBA, 3, SIN_PRECIO, "2026-10-12")));

        cuandoSeArmaElPanel();

        entoncesElPanelTieneMetricas();
        entoncesVendio("0");
        entoncesNoHayTicketNiMargen();
    }

    // --- sin datos ---

    @Test
    @DisplayName("Sin ventas en el período el panel está vacío: sin métricas ni semanas, no ceros")
    void sinVentasEnElPeriodoElPanelEstaVacio() {
        dadoUnAlmacen(unAlmacenQueVendio(yerba(), unaVenta(YERBA, 10, 3400, "2026-08-01")));

        cuandoSeArmaElPanel();

        entoncesElPanelEstaVacio();
        entoncesElPanelEsDelPeriodo(PERIODO);
    }

    @Test
    @DisplayName("Sin tabla de ventas el panel está vacío")
    void sinTablaDeVentasElPanelEstaVacio() {
        dadoUnAlmacen(unAlmacenSinTablas());

        cuandoSeArmaElPanel();

        entoncesElPanelEstaVacio();
    }

    private void dadoUnAlmacen(DatosDeProyecto delAlmacen) {
        datos = delAlmacen;
    }

    private void dadoElPeriodo(PeriodoResumen delPanel) {
        periodo = delPanel;
    }

    private void cuandoSeArmaElPanel() {
        panel = PanelNegocio.armar(datos, periodo, HOY);
    }

    private ResumenNegocio elResumenDelMismoPeriodo() {
        return ResumenNegocio.armar("", datos, AnalisisZona.sinZona(), List.of(), periodo, HOY);
    }

    private void entoncesElPanelEsDelPeriodo(PeriodoResumen esperado) {
        assertThat(panel.periodo()).isEqualTo(esperado);
    }

    private void entoncesElPanelTieneMetricas() {
        assertThat(panel.vacio()).isFalse();
    }

    private void entoncesElPanelEstaVacio() {
        assertThat(panel.vacio()).isTrue();
        assertThat(panel.metricas()).isNull();
        assertThat(panel.flujoDeCaja()).isEmpty();
    }

    private void entoncesLasMetricasCoincidenConElResumen() {
        ResumenNegocio resumen = elResumenDelMismoPeriodo();
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo(resumen.ventas().ingresos());
        assertThat(panel.metricas().gastos()).isEqualByComparingTo(resumen.gastos().total());
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo(resumen.clientes().fiado());
        assertThat(panel.metricas().variacion()).isEqualByComparingTo(resumen.ventas().variacion());
    }

    private void entoncesVendio(String vendiste) {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo(vendiste);
    }

    private void entoncesVendioComproYGasto(String vendiste, String mercaderia, String gastos) {
        entoncesVendio(vendiste);
        entoncesComproMercaderiaPor(mercaderia);
        entoncesGasto(gastos);
    }

    private void entoncesComproMercaderiaPor(String mercaderia) {
        assertThat(panel.metricas().mercaderia()).isEqualByComparingTo(mercaderia);
    }

    private void entoncesGasto(String gastos) {
        assertThat(panel.metricas().gastos()).isEqualByComparingTo(gastos);
    }

    private void entoncesNoSeSabenLosGastosNiLaMercaderia() {
        assertThat(panel.metricas().gastos()).isNull();
        assertThat(panel.metricas().mercaderia()).isNull();
    }

    private void entoncesLeDeben(String teDeben) {
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo(teDeben);
    }

    private void entoncesLeQueda(String queda) {
        assertThat(panel.metricas().queda()).isEqualByComparingTo(queda);
    }

    private void entoncesElMargenYElTicketPromedioSon(String margen, String ticketPromedio) {
        assertThat(panel.metricas().margen()).isEqualByComparingTo(margen);
        assertThat(panel.metricas().ticketPromedio()).isEqualByComparingTo(ticketPromedio);
    }

    private void entoncesNoHayMargen() {
        assertThat(panel.metricas().margen()).isNull();
    }

    private void entoncesElTicketPromedioEs(String ticketPromedio) {
        assertThat(panel.metricas().ticketPromedio()).isEqualByComparingTo(ticketPromedio);
    }

    private void entoncesNoHayTicketNiMargen() {
        assertThat(panel.metricas().ticketPromedio()).isNull();
        assertThat(panel.metricas().margen()).isNull();
    }

    private void entoncesLaVariacionEs(String variacion) {
        assertThat(panel.metricas().variacion()).isEqualByComparingTo(variacion);
    }

    private void entoncesNoHayVariacion() {
        assertThat(panel.metricas().variacion()).isNull();
    }

    private void entoncesElFlujoTieneSemanas(int semanas) {
        assertThat(panel.flujoDeCaja()).hasSize(semanas);
    }

    private void entoncesLaPrimeraSemanaSalieron(String salidas) {
        assertThat(panel.flujoDeCaja().getFirst().salidas()).isEqualByComparingTo(salidas);
    }

    private void entoncesEntroEnTotal(String entradas) {
        BigDecimal total = panel.flujoDeCaja().stream().map(SemanaDeCaja::entradas).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo(entradas);
    }

    private void entoncesLaSemana(int numero, String desde, String hasta, String entradas, String salidas,
                                  String saldo, String acumulado) {
        SemanaDeCaja semana = panel.flujoDeCaja().get(numero);
        assertThat(semana.desde()).isEqualTo(LocalDate.parse(desde));
        assertThat(semana.hasta()).isEqualTo(LocalDate.parse(hasta));
        assertThat(semana.entradas()).isEqualByComparingTo(entradas);
        assertThat(semana.salidas()).isEqualByComparingTo(salidas);
        assertThat(semana.saldo()).isEqualByComparingTo(saldo);
        assertThat(semana.saldoAcumulado()).isEqualByComparingTo(acumulado);
    }
}
