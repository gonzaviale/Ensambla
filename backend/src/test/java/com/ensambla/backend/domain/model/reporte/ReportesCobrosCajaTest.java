package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.caja.CobroCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.CajaTestData;
import com.ensambla.backend.support.CobroCajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ReportesCobrosCajaTest {

    private static final LocalDate HOY = ZonaNegocio.dia(CajaTestData.AHORA).plusDays(1);
    private static final PeriodoResumen PERIODO = new PeriodoResumen(HOY.minusDays(1), HOY);

    private DatosDeProyecto datos;
    private PanelNegocio panel;
    private ResumenNegocio resumen;

    @Test
    @DisplayName("El pago parcial cuenta como dinero recibido y sólo el saldo aparece en Te deben")
    void elPagoParcialCuentaComoDineroRecibidoYSoloElSaldoApareceEnTeDeben() {
        dadoDatosDeUnaVentaConEntregaYSinCobros();

        cuandoSeArmanElPanelYElResumen();

        entoncesElDineroRecibidoEsLaEntregaYElSaldoApareceEnTeDeben();
    }

    @Test
    @DisplayName("Un cobro posterior reduce el saldo sin duplicar artículos ni la entrega inicial")
    void unCobroPosteriorReduceElSaldoSinDuplicarArticulosNiLaEntregaInicial() {
        dadoDatosDeUnaVentaConUnCobroPosterior();

        cuandoSeArmanElPanelYElResumen();

        entoncesElSaldoBajaSinDuplicarArticulosNiLaEntregaInicial();
    }

    @Test
    @DisplayName("Completar una deuda elimina el saldo aunque las filas originales sigan marcadas a fiado")
    void completarUnaDeudaEliminaElSaldoAunqueLasFilasOriginalesSiganMarcadasAFiado() {
        dadoDatosDeUnaDeudaCompletada();

        cuandoSeArmaElResumen();

        entoncesNoQuedaSaldoNiVentasFiadas();
    }

    @Test
    @DisplayName("Un fiado sin entrega no genera entrada de dinero")
    void unFiadoSinEntregaNoGeneraEntradaDeDinero() {
        dadoDatosDeUnFiadoSinEntrega();

        cuandoSeArmaElPanelDelPeriodo(PERIODO);

        entoncesNoEntraDineroYTodoSeDebe();
    }

    @Test
    @DisplayName("Una semana sólo con cobros de ventas anteriores tiene flujo aunque no haya ventas nuevas")
    void unaSemanaSoloConCobrosDeVentasAnterioresTieneFlujoAunqueNoHayaVentasNuevas() {
        dadoDatosDeUnaVentaConUnCobroPosterior();

        cuandoSeArmaElPanelDelPeriodo(new PeriodoResumen(HOY, HOY));

        entoncesElPanelTieneFlujoPorElCobroSinVentasNuevas();
    }

    @Test
    @DisplayName("Un período sin ventas ni cobros reales conserva el panel vacío")
    void unPeriodoSinVentasNiCobrosRealesConservaElPanelVacio() {
        dadoDatosDeUnaVentaConUnCobroPosterior();

        cuandoSeArmaElPanelDelPeriodo(new PeriodoResumen(HOY.plusDays(1), HOY.plusDays(1)));

        entoncesElPanelEstaVacio();
    }

    @Test
    @DisplayName("Las anotaciones manuales a fiado siguen sumando su importe junto al saldo de Caja")
    void lasAnotacionesManualesAFiadoSiguenSumandoSuImporteJuntoAlSaldoDeCaja() {
        dadoUnaVentaDeCajaYUnaAnotacionManualAFiado();

        cuandoSeArmaElResumen();

        entoncesElFiadoSumaLaAnotacionManualJuntoAlSaldoDeCaja();
    }

    @Test
    @DisplayName("Una venta pagada de Caja no vuelve a sumar el importe desde sus filas de artículos")
    void unaVentaPagadaDeCajaNoVuelveASumarElImporteDesdeSusFilasDeArticulos() {
        dadoUnaVentaPagadaDeCajaConSusArticulosSinFiado();

        cuandoSeArmaElPanelDelPeriodo(PERIODO);

        entoncesElDineroEntraUnaSolaVezYNadaSeDebe();
    }

    // --- dado ---
    private void dadoDatosDeUnaVentaConEntregaYSinCobros() {
        datos = dadoDatos(CobroCajaTestData.venta(), List.of());
    }

    private void dadoDatosDeUnaVentaConUnCobroPosterior() {
        CobroCaja cobro = CobroCajaTestData.cobro();
        VentaCaja venta = CobroCajaTestData.venta().conCobro(cobro.aplicaciones().getFirst().pagos());
        datos = dadoDatos(venta, List.of(cobro));
    }

    private void dadoDatosDeUnaDeudaCompletada() {
        VentaCaja venta = CobroCajaTestData.venta().conCobro(List.of(
                new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("1200"))));
        datos = dadoDatos(venta, List.of());
    }

    private void dadoDatosDeUnFiadoSinEntrega() {
        datos = dadoDatos(CobroCajaTestData.venta(CajaTestData.OPERACION, "0"), List.of());
    }

    private void dadoUnaVentaDeCajaYUnaAnotacionManualAFiado() {
        datos = dadoDatos(CobroCajaTestData.venta(), List.of());
        BloqueVentas ventas = datos.ventas().getFirst();
        Registro manual = ventas.registros().getFirst();
        manual.setOrigen("Carga manual");
    }

    private void dadoUnaVentaPagadaDeCajaConSusArticulosSinFiado() {
        datos = dadoDatos(CobroCajaTestData.venta(CajaTestData.OPERACION, "2000"), List.of());
        BloqueVentas ventas = datos.ventas().getFirst();
        ventas.registros().forEach(fila -> CajaTestData.poner(ventas.bloque(), fila, "credit", false));
    }

    private DatosDeProyecto dadoDatos(VentaCaja venta, List<CobroCaja> cobros) {
        Bloque bloque = CajaTestData.sistema().proyecto().getBloques().get(1);
        List<Campo> campos = new ArrayList<>(bloque.getCampos());
        campos.add(Campo.builder().id(CampoId.nuevo()).bloqueId(bloque.getId()).clave("credit")
                .tipo(TipoCampo.SI_NO).build());
        bloque.setCampos(campos);
        List<Registro> filas = List.of(fila(bloque, venta), fila(bloque, venta));
        BloqueVentas ventas = new BloqueVentas(bloque, campos.stream()
                .collect(Collectors.toMap(Campo::getClave, campo -> campo)), filas);
        return new DatosDeProyecto("Negocio", List.of(), List.of(ventas), List.of(), Map.of(), List.of(),
                List.of(venta), cobros);
    }

    private Registro fila(Bloque bloque, VentaCaja venta) {
        Registro fila = Registro.nuevo(CajaTestData.NEGOCIO, bloque.getId(), new LinkedHashMap<>(), CajaTestData.AHORA);
        fila.setOrigen("Caja " + venta.id());
        CajaTestData.poner(bloque, fila, "product", CajaTestData.PRODUCTO.toString());
        CajaTestData.poner(bloque, fila, "quantity", new BigDecimal("0.25"));
        CajaTestData.poner(bloque, fila, "price", new BigDecimal("4000"));
        CajaTestData.poner(bloque, fila, "date", ZonaNegocio.dia(venta.creadoEn()).toString());
        CajaTestData.poner(bloque, fila, "credit", true);
        return fila;
    }

    // --- cuando ---
    private void cuandoSeArmanElPanelYElResumen() {
        cuandoSeArmaElPanelDelPeriodo(PERIODO);
        cuandoSeArmaElResumen();
    }

    private void cuandoSeArmaElPanelDelPeriodo(PeriodoResumen periodo) {
        panel = PanelNegocio.armar(datos, periodo, HOY);
    }

    private void cuandoSeArmaElResumen() {
        resumen = ResumenNegocio.armar("Negocio", datos, AnalisisZona.sinZona(), List.of(), PERIODO, HOY);
    }

    // --- entonces ---
    private void entoncesElDineroRecibidoEsLaEntregaYElSaldoApareceEnTeDeben() {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("800");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("1200");
        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("1200");
        assertThat(resumen.clientes().ventasFiadas()).isOne();
    }

    private void entoncesElSaldoBajaSinDuplicarArticulosNiLaEntregaInicial() {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("1400");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("600");
        assertThat(resumen.ventas().cantidad()).isEqualTo(2);
    }

    private void entoncesNoQuedaSaldoNiVentasFiadas() {
        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("0");
        assertThat(resumen.clientes().ventasFiadas()).isZero();
    }

    private void entoncesNoEntraDineroYTodoSeDebe() {
        assertThat(panel.metricas().queda()).isEqualByComparingTo("0");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("2000");
    }

    private void entoncesElPanelTieneFlujoPorElCobroSinVentasNuevas() {
        assertThat(panel.vacio()).isFalse();
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("0");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("600");
        assertThat(panel.flujoDeCaja().getFirst().entradas()).isEqualByComparingTo("600");
    }

    private void entoncesElPanelEstaVacio() {
        assertThat(panel.vacio()).isTrue();
    }

    private void entoncesElFiadoSumaLaAnotacionManualJuntoAlSaldoDeCaja() {
        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("2200");
        assertThat(resumen.clientes().ventasFiadas()).isEqualTo(2);
    }

    private void entoncesElDineroEntraUnaSolaVezYNadaSeDebe() {
        assertThat(panel.metricas().queda()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("0");
    }
}
