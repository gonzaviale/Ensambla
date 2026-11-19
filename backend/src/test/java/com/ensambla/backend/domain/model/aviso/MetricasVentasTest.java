package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasVentasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int PERIODO = 30;
    private static final RegistroId PRODUCTO = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId OTRO_PRODUCTO = RegistroId.de("d0000000-0000-4000-8000-000000000002");

    private final MetricasVentas metricasVentas = new MetricasVentas(PERIODO, new BigDecimal("0.5"));
    private final List<BloqueVentas> tablasDeVentas = new ArrayList<>();
    private final List<MetricaProducto> todosLosProductos = new ArrayList<>();

    private Map<RegistroId, MetricaProducto> metricas;
    private MetricaProducto metrica;
    private List<VentaProducto> ventasValidas;
    private MetricaProducto producto;
    private int periodo;
    private boolean bajaRotacion;

    @Test
    @DisplayName("Expone el período configurado")
    void exponeElPeriodoConfigurado() {
        cuandoSePideElPeriodo();

        entoncesElPeriodoEsElConfigurado();
    }

    @Test
    @DisplayName("Suma sólo las unidades del período, sin contar el día que lo abre")
    void sumaSoloLasUnidadesDelPeriodoSinContarElDiaQueLoAbre() {
        dadoVentasDentroYFueraDelPeriodo();

        cuandoSeCalculanLasMetricas();

        entoncesLasUnidadesDelPeriodoSon("5");
    }

    @Test
    @DisplayName("Promedia las unidades por período desde la primera venta")
    void promediaLasUnidadesPorPeriodoDesdeLaPrimeraVenta() {
        dadoDiezUnidadesVendidasEnSesentaYUnDias();

        cuandoSeCalculanLasMetricas();

        entoncesElPromedioPorPeriodoEs("4.92");
    }

    @Test
    @DisplayName("Con menos de un período de historia el promedio es el total")
    void conMenosDeUnPeriodoDeHistoriaElPromedioEsElTotal() {
        dadoUnaVentaDeSeisUnidadesHaceNueveDias();

        cuandoSeCalculanLasMetricas();

        entoncesElPromedioPorPeriodoEs("6");
    }

    @Test
    @DisplayName("Calcula el precio promedio histórico ponderado por cantidad y la última venta")
    void calculaElPrecioPromedioHistoricoPonderadoPorCantidadYLaUltimaVenta() {
        dadoDiezUnidadesVendidasEnSesentaYUnDias();

        cuandoSeCalculanLasMetricas();

        entoncesElPrecioHistoricoPonderadoEs("230.00");
        entoncesLaUltimaVentaFueElDieciochoDeOctubre();
    }

    @Test
    @DisplayName("Sin precio en ninguna venta el precio histórico queda vacío")
    void sinPrecioEnNingunaVentaElPrecioHistoricoQuedaVacio() {
        dadoVentasSinPrecio();

        cuandoSeCalculanLasMetricas();

        entoncesNoHayPrecioHistorico();
        entoncesLasUnidadesDelPeriodoSon("3");
    }

    @Test
    @DisplayName("Ignora las filas sin producto, cantidad o fecha válidos y las ventas del futuro")
    void ignoraLasFilasSinProductoCantidadOFechaValidosYLasVentasDelFuturo() {
        dadoFilasInvalidasYUnaVentaValida();

        cuandoSeCalculanLasMetricas();

        entoncesSoloHayMetricasDelProducto();
        entoncesLasUnidadesDelPeriodoSon("4");
    }

    @Test
    @DisplayName("Una venta anterior al período cuenta en el historial pero no en las unidades del período")
    void unaVentaAnteriorAlPeriodoCuentaEnElHistorialPeroNoEnLasUnidadesDelPeriodo() {
        dadoUnaVentaAnteriorAlPeriodo();

        cuandoSeCalculanLasMetricas();

        entoncesLasUnidadesDelPeriodoSon("0");
        entoncesLaUltimaVentaFueElPrimeroDeAgosto();
    }

    @Test
    @DisplayName("Lee las ventas de todas las tablas de ventas del negocio")
    void leeLasVentasDeTodasLasTablasDeVentasDelNegocio() {
        dadoDosTablasDeVentasDeProductosDistintos();

        cuandoSeCalculanLasMetricas();

        entoncesHayMetricasDeAmbosProductos();
    }

    @Test
    @DisplayName("Sin filas de ventas no hay métricas")
    void sinFilasDeVentasNoHayMetricas() {
        dadoUnaTablaDeVentasVacia();

        cuandoSeCalculanLasMetricas();

        entoncesNoHayMetricas();
    }

    @Test
    @DisplayName("Con alguna venta válida hay métricas")
    void conAlgunaVentaValidaHayMetricas() {
        dadoUnaVentaDeUnaUnidadAyer();

        cuandoSeCalculanLasMetricas();

        entoncesHayMetricas();
    }

    @Test
    @DisplayName("Devuelve las ventas válidas hasta hoy con su cliente")
    void devuelveLasVentasValidasHastaHoyConSuCliente() {
        dadoVentasConClienteSinClienteYDelFuturo();

        cuandoSePidenLasVentasValidas();

        entoncesSeDevuelvenLasVentasHastaHoyConSuCliente();
    }

    @Test
    @DisplayName("Un producto sin ventas en el período rota poco")
    void unProductoSinVentasEnElPeriodoRotaPoco() {
        dadoUnProductoSinVentas();

        cuandoSeEvaluaLaRotacion();

        entoncesRotaPoco();
    }

    @Test
    @DisplayName("Rota poco si vendió la mitad de la mediana de los demás o menos")
    void rotaPocoSiVendioLaMitadDeLaMedianaDeLosDemasOMenos() {
        dadoUnProductoConVentasDe("3");
        dadoOtrosProductosConVentasDe("10", "4", "6");

        cuandoSeEvaluaLaRotacion();

        entoncesRotaPoco();
    }

    @Test
    @DisplayName("No rota poco si vendió más de la mitad de la mediana de los demás")
    void noRotaPocoSiVendioMasDeLaMitadDeLaMedianaDeLosDemas() {
        dadoUnProductoConVentasDe("4");
        dadoOtrosProductosConVentasDe("10", "4", "6");

        cuandoSeEvaluaLaRotacion();

        entoncesNoRotaPoco();
    }

    @Test
    @DisplayName("Con una cantidad par de productos la mediana es el promedio de los dos del medio")
    void conUnaCantidadParDeProductosLaMedianaEsElPromedioDeLosDosDelMedio() {
        dadoUnProductoConVentasDe("3");
        dadoOtrosProductosConVentasDe("4", "10");

        cuandoSeEvaluaLaRotacion();

        entoncesRotaPoco();
    }

    @Test
    @DisplayName("Con una cantidad par de productos no rota poco si supera la mitad de esa mediana")
    void conUnaCantidadParDeProductosNoRotaPocoSiSuperaLaMitadDeEsaMediana() {
        dadoUnProductoConVentasDe("4");
        dadoOtrosProductosConVentasDe("4", "10");

        cuandoSeEvaluaLaRotacion();

        entoncesNoRotaPoco();
    }

    @Test
    @DisplayName("No cuenta a los productos que no vendieron nada para calcular la mediana")
    void noCuentaALosProductosQueNoVendieronNadaParaCalcularLaMediana() {
        dadoUnProductoConVentasDe("6");
        dadoOtroProductoSinVentas();
        dadoOtrosProductosConVentasDe("10");

        cuandoSeEvaluaLaRotacion();

        entoncesNoRotaPoco();
    }

    @Test
    @DisplayName("Sin otros productos con ventas no hay con qué comparar y no rota poco")
    void sinOtrosProductosConVentasNoHayConQueCompararYNoRotaPoco() {
        dadoUnProductoConVentasDe("3");
        dadoOtroProductoSinVentas();

        cuandoSeEvaluaLaRotacion();

        entoncesNoRotaPoco();
    }

    // --- dado ---

    private void dadoVentasDentroYFueraDelPeriodo() {
        dadoUnaTablaDeVentas(
                venta(PRODUCTO, 2, 100, "2026-10-18"),
                venta(PRODUCTO, 3, 200, "2026-09-20"),
                venta(PRODUCTO, 5, 300, "2026-09-19"));
    }

    private void dadoDiezUnidadesVendidasEnSesentaYUnDias() {
        dadoUnaTablaDeVentas(
                venta(PRODUCTO, 2, 100, "2026-10-18"),
                venta(PRODUCTO, 3, 200, "2026-10-01"),
                venta(PRODUCTO, 5, 300, "2026-08-20"));
    }

    private void dadoUnaVentaDeSeisUnidadesHaceNueveDias() {
        dadoUnaTablaDeVentas(venta(PRODUCTO, 6, 100, "2026-10-10"));
    }

    private void dadoVentasSinPrecio() {
        dadoUnaTablaDeVentas(
                AvisoTestData.venta(PRODUCTO.toString(), 2, null, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 0, "2026-10-17", null));
    }

    private void dadoFilasInvalidasYUnaVentaValida() {
        dadoUnaTablaDeVentas(
                AvisoTestData.venta("no-es-un-id", 1, 100, "2026-10-18", null),
                AvisoTestData.venta(7, 1, 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 0, 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), "dos", 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "ayer", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, 20261018, null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-20", null),
                venta(PRODUCTO, 4, 100, "2026-10-19"));
    }

    private void dadoUnaVentaAnteriorAlPeriodo() {
        dadoUnaTablaDeVentas(venta(PRODUCTO, 2, 100, "2026-08-01"));
    }

    private void dadoDosTablasDeVentasDeProductosDistintos() {
        dadoUnaTablaDeVentas(venta(PRODUCTO, 1, 100, "2026-10-18"));
        dadoUnaTablaDeVentas(venta(OTRO_PRODUCTO, 2, 100, "2026-10-18"));
    }

    private void dadoUnaTablaDeVentasVacia() {
        dadoUnaTablaDeVentas();
    }

    private void dadoUnaVentaDeUnaUnidadAyer() {
        dadoUnaTablaDeVentas(venta(PRODUCTO, 1, 100, "2026-10-18"));
    }

    private void dadoVentasConClienteSinClienteYDelFuturo() {
        dadoUnaTablaDeVentas(
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-18", "  Doña Rosa "),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-18", "   "),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-30", "Doña Rosa"));
    }

    private void dadoUnaTablaDeVentas(Registro... filas) {
        tablasDeVentas.add(AvisoTestData.ventas(filas));
    }

    private void dadoUnProductoSinVentas() {
        producto = MetricaProducto.sinVentas(PRODUCTO);
        todosLosProductos.add(producto);
    }

    private void dadoUnProductoConVentasDe(String unidades) {
        producto = metricaDe(PRODUCTO, unidades);
        todosLosProductos.add(producto);
    }

    private void dadoOtrosProductosConVentasDe(String... unidades) {
        for (String cantidad : unidades) {
            todosLosProductos.add(metricaDe(RegistroId.nuevo(), cantidad));
        }
    }

    private void dadoOtroProductoSinVentas() {
        todosLosProductos.add(MetricaProducto.sinVentas(RegistroId.nuevo()));
    }

    // --- cuando ---

    private void cuandoSePideElPeriodo() {
        periodo = metricasVentas.periodoDias();
    }

    private void cuandoSeCalculanLasMetricas() {
        metricas = metricasVentas.calcular(tablasDeVentas, HOY);
        metrica = metricas.get(PRODUCTO);
    }

    private void cuandoSePidenLasVentasValidas() {
        ventasValidas = metricasVentas.ventasValidas(tablasDeVentas, HOY);
    }

    private void cuandoSeEvaluaLaRotacion() {
        bajaRotacion = metricasVentas.esBajaRotacion(producto, todosLosProductos);
    }

    // --- entonces ---

    private void entoncesElPeriodoEsElConfigurado() {
        assertThat(periodo).isEqualTo(PERIODO);
    }

    private void entoncesLasUnidadesDelPeriodoSon(String esperadas) {
        assertThat(metrica.unidadesPeriodo()).isEqualByComparingTo(esperadas);
    }

    private void entoncesElPromedioPorPeriodoEs(String esperado) {
        assertThat(metrica.promedioPorPeriodo()).isEqualByComparingTo(esperado);
    }

    private void entoncesElPrecioHistoricoPonderadoEs(String esperado) {
        assertThat(metrica.precioPromedioHistorico()).isEqualByComparingTo(esperado);
        assertThat(metrica.productoId()).isEqualTo(PRODUCTO);
    }

    private void entoncesNoHayPrecioHistorico() {
        assertThat(metrica.precioPromedioHistorico()).isNull();
    }

    private void entoncesLaUltimaVentaFueElDieciochoDeOctubre() {
        assertThat(metrica.ultimaVenta()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    private void entoncesLaUltimaVentaFueElPrimeroDeAgosto() {
        assertThat(metrica.ultimaVenta()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    private void entoncesSoloHayMetricasDelProducto() {
        assertThat(metricas).containsOnlyKeys(PRODUCTO);
    }

    private void entoncesHayMetricasDeAmbosProductos() {
        assertThat(metricas).containsOnlyKeys(PRODUCTO, OTRO_PRODUCTO);
    }

    private void entoncesNoHayMetricas() {
        assertThat(metricas).isEmpty();
    }

    private void entoncesHayMetricas() {
        assertThat(metricas).isNotEmpty();
    }

    private void entoncesSeDevuelvenLasVentasHastaHoyConSuCliente() {
        assertThat(ventasValidas).extracting(VentaProducto::cliente).containsExactly("Doña Rosa", null);
    }

    private void entoncesRotaPoco() {
        assertThat(bajaRotacion).isTrue();
    }

    private void entoncesNoRotaPoco() {
        assertThat(bajaRotacion).isFalse();
    }

    // --- datos ---

    private static Registro venta(RegistroId producto, Object cantidad, Object precio, String fecha) {
        return AvisoTestData.venta(producto.toString(), cantidad, precio, fecha, null);
    }

    private static MetricaProducto metricaDe(RegistroId productoId, String unidades) {
        BigDecimal cantidad = new BigDecimal(unidades);
        return new MetricaProducto(productoId, cantidad, cantidad, null, HOY);
    }
}
