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

    @Test
    @DisplayName("El pago parcial cuenta como dinero recibido y sólo el saldo aparece en Te deben")
    void armarShouldUseInitialPaymentAndOutstandingBalance() {
        DatosDeProyecto datos = givenDatos(CobroCajaTestData.venta(), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);
        ResumenNegocio resumen = resumen(datos);

        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("800");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("1200");
        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("1200");
        assertThat(resumen.clientes().ventasFiadas()).isOne();
    }

    @Test
    @DisplayName("Un cobro posterior reduce el saldo sin duplicar artículos ni la entrega inicial")
    void armarShouldReduceDebtWithoutDuplicatingCashOrSales() {
        CobroCaja cobro = CobroCajaTestData.cobro();
        VentaCaja venta = CobroCajaTestData.venta().conCobro(cobro.aplicaciones().getFirst().pagos());
        DatosDeProyecto datos = givenDatos(venta, List.of(cobro));

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("1400");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("600");
        assertThat(resumen(datos).ventas().cantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("Completar una deuda elimina el saldo aunque las filas originales sigan marcadas a fiado")
    void armarShouldClearSettledDebt() {
        VentaCaja venta = CobroCajaTestData.venta().conCobro(List.of(
                new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("1200"))));

        ResumenNegocio resumen = resumen(givenDatos(venta, List.of()));

        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("0");
        assertThat(resumen.clientes().ventasFiadas()).isZero();
    }

    @Test
    @DisplayName("Un fiado sin entrega no genera entrada de dinero")
    void armarShouldKeepUnpaidSalesOutOfCash() {
        DatosDeProyecto datos = givenDatos(CobroCajaTestData.venta(CajaTestData.OPERACION, "0"), List.of());

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().queda()).isEqualByComparingTo("0");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("2000");
    }

    @Test
    @DisplayName("Una semana sólo con cobros de ventas anteriores tiene flujo aunque no haya ventas nuevas")
    void armarShouldIncludeCollectionsInTheirOwnPeriod() {
        DatosDeProyecto datos = givenDatos(CobroCajaTestData.venta(), List.of(CobroCajaTestData.cobro()));

        PanelNegocio panel = PanelNegocio.armar(datos, new PeriodoResumen(HOY, HOY), HOY);

        assertThat(panel.vacio()).isFalse();
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("0");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("600");
        assertThat(panel.flujoDeCaja().getFirst().entradas()).isEqualByComparingTo("600");
    }

    @Test
    @DisplayName("Un período sin ventas ni cobros reales conserva el panel vacío")
    void armarShouldExcludeOutOfPeriodCollections() {
        DatosDeProyecto datos = givenDatos(CobroCajaTestData.venta(), List.of(CobroCajaTestData.cobro()));

        PanelNegocio panel = PanelNegocio.armar(datos, new PeriodoResumen(HOY.plusDays(1), HOY.plusDays(1)), HOY);

        assertThat(panel.vacio()).isTrue();
    }

    @Test
    @DisplayName("Las anotaciones manuales a fiado siguen sumando su importe junto al saldo de Caja")
    void armarShouldPreserveManualCreditSales() {
        DatosDeProyecto originales = givenDatos(CobroCajaTestData.venta(), List.of());
        BloqueVentas ventas = originales.ventas().getFirst();
        Registro manual = ventas.registros().getFirst();
        manual.setOrigen("Carga manual");

        ResumenNegocio resumen = resumen(originales);

        assertThat(resumen.clientes().fiado()).isEqualByComparingTo("2200");
        assertThat(resumen.clientes().ventasFiadas()).isEqualTo(2);
    }

    @Test
    @DisplayName("Una venta pagada de Caja no vuelve a sumar el importe desde sus filas de artículos")
    void armarShouldCountPaidCheckoutOnce() {
        DatosDeProyecto datos = givenDatos(CobroCajaTestData.venta(CajaTestData.OPERACION, "2000"), List.of());
        BloqueVentas ventas = datos.ventas().getFirst();
        ventas.registros().forEach(fila -> CajaTestData.poner(ventas.bloque(), fila, "credit", false));

        PanelNegocio panel = PanelNegocio.armar(datos, PERIODO, HOY);

        assertThat(panel.metricas().queda()).isEqualByComparingTo("2000");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("0");
    }

    // --- arrange ---
    private DatosDeProyecto givenDatos(VentaCaja venta, List<CobroCaja> cobros) {
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

    private ResumenNegocio resumen(DatosDeProyecto datos) {
        return ResumenNegocio.armar("Negocio", datos, AnalisisZona.sinZona(), List.of(), PERIODO, HOY);
    }
}
