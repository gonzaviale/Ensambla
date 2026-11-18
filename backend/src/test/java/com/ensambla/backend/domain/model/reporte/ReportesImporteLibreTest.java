package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.caja.LineaVentaCaja;
import com.ensambla.backend.domain.model.caja.MedioPagoCaja;
import com.ensambla.backend.domain.model.caja.PagoCaja;
import com.ensambla.backend.domain.model.caja.VentaCaja;
import com.ensambla.backend.domain.model.common.ZonaNegocio;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class ReportesImporteLibreTest {
    private static final LocalDate HOY = ZonaNegocio.dia(CajaTestData.AHORA);
    private static final PeriodoResumen PERIODO = new PeriodoResumen(HOY, HOY);

    @Test
    @DisplayName("Inicio suma una venta libre pagada una sola vez sin inventar producto ni costo")
    void armarShouldIncludePaidFreeAmountOnce() {
        DatosDeProyecto datos = givenImportePagado();

        PanelNegocio panel = whenSeConsultaPanel(datos);
        ResumenNegocio resumen = whenSeConsultaResumen(datos);

        thenCuentaImporteSinProducto(panel, resumen);
    }

    @Test
    @DisplayName("Un importe libre parcial separa lo vendido, lo cobrado y lo pendiente")
    void armarShouldSeparatePartialFreeSaleFromCashAndDebt() {
        DatosDeProyecto datos = givenImporteParcial();

        PanelNegocio panel = whenSeConsultaPanel(datos);

        thenConservaEntregaYSaldo(panel);
    }

    @Test
    @DisplayName("Un negocio con solo un importe libre fiado muestra venta y deuda sin dinero recibido")
    void armarShouldShowUnpaidFreeSaleWithoutCash() {
        DatosDeProyecto datos = givenImporteFiado();

        PanelNegocio panel = whenSeConsultaPanel(datos);

        thenMuestraFiadoSinCobro(panel);
    }

    @Test
    @DisplayName("Un importe libre respeta el período de su venta original")
    void armarShouldExcludeFreeAmountOutsidePeriod() {
        DatosDeProyecto datos = givenImporteFiado();

        PanelNegocio panel = whenSeConsultaOtroDia(datos);

        thenNoMuestraVentasAjenasAlPeriodo(panel);
    }

    private DatosDeProyecto givenImportePagado() {
        return datosConPagos(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("2500"))), null, null);
    }

    private DatosDeProyecto givenImporteParcial() {
        return datosConPagos(List.of(new PagoCaja(MedioPagoCaja.TRANSFERENCIA, new BigDecimal("500"))),
                CajaTestData.CLIENTE, "Ana López");
    }

    private DatosDeProyecto givenImporteFiado() {
        return datosConPagos(List.of(), CajaTestData.CLIENTE, "Ana López");
    }

    private DatosDeProyecto datosConPagos(List<PagoCaja> pagos, RegistroId cliente, String nombre) {
        VentaCaja venta = VentaCaja.nueva(CajaTestData.OPERACION, CajaTestData.NEGOCIO, CajaTestData.PROYECTO,
                CajaTestData.INVENTARIO, "importe", List.of(LineaVentaCaja.importeLibre(
                        "Servicio", BigDecimal.ONE, new BigDecimal("2500"))),
                MedioPagoCaja.TRANSFERENCIA, null, CajaTestData.AHORA, pagos, cliente, nombre);
        return new DatosDeProyecto("Caja", List.of(), List.of(), List.of(), Map.of(), List.of(), List.of(venta), List.of());
    }

    private PanelNegocio whenSeConsultaPanel(DatosDeProyecto datos) {
        return PanelNegocio.armar(datos, PERIODO, HOY);
    }

    private PanelNegocio whenSeConsultaOtroDia(DatosDeProyecto datos) {
        LocalDate manana = HOY.plusDays(1);
        return PanelNegocio.armar(datos, new PeriodoResumen(manana, manana), manana);
    }

    private ResumenNegocio whenSeConsultaResumen(DatosDeProyecto datos) {
        return ResumenNegocio.armar("Negocio", datos, AnalisisZona.sinZona(), List.of(), PERIODO, HOY);
    }

    private void thenCuentaImporteSinProducto(PanelNegocio panel, ResumenNegocio resumen) {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2500");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("2500");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("0");
        assertThat(panel.metricas().ticketPromedio()).isEqualByComparingTo("2500");
        assertThat(panel.metricas().margen()).isNull();
        assertThat(resumen.ventas().cantidad()).isOne();
        assertThat(resumen.masVendidos()).isEmpty();
        assertThat(resumen.stock().productos()).isZero();
        assertThat(resumen.ventas().gananciaEstimada()).isNull();
    }

    private void thenConservaEntregaYSaldo(PanelNegocio panel) {
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2500");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("500");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("2000");
    }

    private void thenMuestraFiadoSinCobro(PanelNegocio panel) {
        assertThat(panel.vacio()).isFalse();
        assertThat(panel.metricas().vendiste()).isEqualByComparingTo("2500");
        assertThat(panel.metricas().queda()).isEqualByComparingTo("0");
        assertThat(panel.metricas().teDeben()).isEqualByComparingTo("2500");
    }

    private void thenNoMuestraVentasAjenasAlPeriodo(PanelNegocio panel) {
        assertThat(panel.vacio()).isTrue();
    }
}
