package com.ensambla.backend.domain.service.aviso;

import com.ensambla.backend.domain.model.aviso.BloqueVentas;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MetricasVentasServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);
    private static final int PERIODO = 30;
    private static final UUID PRODUCTO = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID OTRO_PRODUCTO = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    private final MetricasVentasService servicio = new MetricasVentasService(PERIODO, new BigDecimal("0.5"));

    @Test
    @DisplayName("Expone el período configurado")
    void periodoDiasShouldReturnConfiguredPeriod() {
        assertThat(servicio.periodoDias()).isEqualTo(PERIODO);
    }

    @Test
    @DisplayName("Suma sólo las unidades del período, sin contar el día que lo abre")
    void calcularShouldSumUnitsOfThePeriodOnly() {
        BloqueVentas ventas = ventas(
                venta(PRODUCTO, 2, 100, "2026-10-18"),
                venta(PRODUCTO, 3, 200, "2026-09-20"),
                venta(PRODUCTO, 5, 300, "2026-09-19"));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        assertThat(metrica.unidadesPeriodo()).isEqualByComparingTo("5");
    }

    @Test
    @DisplayName("Promedia las unidades por período desde la primera venta")
    void calcularShouldAverageUnitsPerPeriodSinceFirstSale() {
        BloqueVentas ventas = ventas(
                venta(PRODUCTO, 2, 100, "2026-10-18"),
                venta(PRODUCTO, 3, 200, "2026-10-01"),
                venta(PRODUCTO, 5, 300, "2026-08-20"));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        // 10 unidades en 61 días = 2,0333 períodos de 30 días
        assertThat(metrica.promedioPorPeriodo()).isEqualByComparingTo("4.92");
    }

    @Test
    @DisplayName("Con menos de un período de historia el promedio es el total")
    void calcularShouldCountLessThanOnePeriodOfHistoryAsOne() {
        BloqueVentas ventas = ventas(venta(PRODUCTO, 6, 100, "2026-10-10"));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        assertThat(metrica.promedioPorPeriodo()).isEqualByComparingTo("6");
    }

    @Test
    @DisplayName("Calcula el precio promedio histórico ponderado por cantidad y la última venta")
    void calcularShouldWeightHistoricPriceByQuantityAndFindLastSale() {
        BloqueVentas ventas = ventas(
                venta(PRODUCTO, 2, 100, "2026-10-18"),
                venta(PRODUCTO, 3, 200, "2026-10-01"),
                venta(PRODUCTO, 5, 300, "2026-08-20"));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        // (2 x 100 + 3 x 200 + 5 x 300) / 10
        assertThat(metrica.precioPromedioHistorico()).isEqualByComparingTo("230.00");
        assertThat(metrica.ultimaVenta()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(metrica.productoId()).isEqualTo(PRODUCTO);
    }

    @Test
    @DisplayName("Sin precio en ninguna venta el precio histórico queda vacío")
    void calcularShouldLeaveHistoricPriceEmptyWhenNoSaleHasPrice() {
        BloqueVentas ventas = ventas(
                AvisoTestData.venta(PRODUCTO.toString(), 2, null, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 0, "2026-10-17", null));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        assertThat(metrica.precioPromedioHistorico()).isNull();
        assertThat(metrica.unidadesPeriodo()).isEqualByComparingTo("3");
    }

    @Test
    @DisplayName("Ignora las filas sin producto, cantidad o fecha válidos y las ventas del futuro")
    void calcularShouldIgnoreInvalidRowsAndFutureSales() {
        BloqueVentas ventas = ventas(
                AvisoTestData.venta("no-es-un-id", 1, 100, "2026-10-18", null),
                AvisoTestData.venta(7, 1, 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 0, 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), "dos", 100, "2026-10-18", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "ayer", null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, 20261018, null),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-20", null),
                venta(PRODUCTO, 4, 100, "2026-10-19"));

        Map<UUID, MetricaProducto> metricas = whenCalcular(ventas);

        assertThat(metricas).containsOnlyKeys(PRODUCTO);
        assertThat(metricas.get(PRODUCTO).unidadesPeriodo()).isEqualByComparingTo("4");
    }

    @Test
    @DisplayName("Una venta anterior al período cuenta en el historial pero no en las unidades del período")
    void calcularShouldKeepProductWithOnlyOldSales() {
        BloqueVentas ventas = ventas(venta(PRODUCTO, 2, 100, "2026-08-01"));

        MetricaProducto metrica = whenCalcular(ventas).get(PRODUCTO);

        assertThat(metrica.unidadesPeriodo()).isEqualByComparingTo("0");
        assertThat(metrica.ultimaVenta()).isEqualTo(LocalDate.of(2026, 8, 1));
    }

    @Test
    @DisplayName("Lee las ventas de todas las tablas de ventas del negocio")
    void calcularShouldReadEverySalesBlock() {
        BloqueVentas una = ventas(venta(PRODUCTO, 1, 100, "2026-10-18"));
        BloqueVentas otra = ventas(venta(OTRO_PRODUCTO, 2, 100, "2026-10-18"));

        Map<UUID, MetricaProducto> metricas = servicio.calcular(List.of(una, otra), HOY, PERIODO);

        assertThat(metricas).containsOnlyKeys(PRODUCTO, OTRO_PRODUCTO);
    }

    @Test
    @DisplayName("Sin filas de ventas no hay métricas ni ventas")
    void hayVentasShouldBeFalseWhenThereAreNoSales() {
        Map<UUID, MetricaProducto> metricas = servicio.calcular(List.of(ventas()), HOY, PERIODO);

        assertThat(metricas).isEmpty();
        assertThat(servicio.hayVentas(metricas)).isFalse();
    }

    @Test
    @DisplayName("Con alguna venta válida hay ventas")
    void hayVentasShouldBeTrueWhenThereIsAValidSale() {
        Map<UUID, MetricaProducto> metricas = whenCalcular(ventas(venta(PRODUCTO, 1, 100, "2026-10-18")));

        assertThat(servicio.hayVentas(metricas)).isTrue();
    }

    @Test
    @DisplayName("Devuelve las ventas válidas hasta hoy con su cliente")
    void ventasValidasShouldReturnReadableSalesWithCustomer() {
        BloqueVentas ventas = ventas(
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-18", "  Doña Rosa "),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-18", "   "),
                AvisoTestData.venta(PRODUCTO.toString(), 1, 100, "2026-10-30", "Doña Rosa"));

        List<VentaProducto> validas = servicio.ventasValidas(List.of(ventas), HOY);

        assertThat(validas).extracting(VentaProducto::cliente).containsExactly("Doña Rosa", null);
    }

    @Test
    @DisplayName("Un producto sin ventas en el período rota poco")
    void esBajaRotacionShouldBeTrueWhenNothingWasSold() {
        MetricaProducto sinVentas = MetricaProducto.sinVentas(PRODUCTO);

        assertThat(servicio.esBajaRotacion(sinVentas, List.of(sinVentas))).isTrue();
    }

    @Test
    @DisplayName("Rota poco si vendió la mitad de la mediana de los demás o menos")
    void esBajaRotacionShouldBeTrueWhenAtOrBelowHalfOfTheMedian() {
        MetricaProducto metrica = metrica(PRODUCTO, "3");

        boolean resultado = servicio.esBajaRotacion(metrica, List.of(metrica, metrica(UUID.randomUUID(), "10"),
                metrica(UUID.randomUUID(), "4"), metrica(UUID.randomUUID(), "6")));

        // mediana de 4, 6 y 10 = 6; la mitad = 3
        assertThat(resultado).isTrue();
    }

    @Test
    @DisplayName("No rota poco si vendió más de la mitad de la mediana de los demás")
    void esBajaRotacionShouldBeFalseWhenAboveHalfOfTheMedian() {
        MetricaProducto metrica = metrica(PRODUCTO, "4");

        boolean resultado = servicio.esBajaRotacion(metrica, List.of(metrica, metrica(UUID.randomUUID(), "10"),
                metrica(UUID.randomUUID(), "4"), metrica(UUID.randomUUID(), "6")));

        assertThat(resultado).isFalse();
    }

    @Test
    @DisplayName("Con una cantidad par de productos la mediana es el promedio de los dos del medio")
    void esBajaRotacionShouldAverageTheMiddlePairWhenCountIsEven() {
        MetricaProducto metrica = metrica(PRODUCTO, "3");
        List<MetricaProducto> todas = List.of(metrica, metrica(UUID.randomUUID(), "4"),
                metrica(UUID.randomUUID(), "10"));

        // mediana de 4 y 10 = 7; la mitad = 3,5
        assertThat(servicio.esBajaRotacion(metrica, todas)).isTrue();
        assertThat(servicio.esBajaRotacion(metrica(PRODUCTO, "4"), todas)).isFalse();
    }

    @Test
    @DisplayName("No cuenta a los productos que no vendieron nada para calcular la mediana")
    void esBajaRotacionShouldIgnoreProductsWithoutSalesInTheMedian() {
        MetricaProducto metrica = metrica(PRODUCTO, "6");
        List<MetricaProducto> todas = List.of(metrica, MetricaProducto.sinVentas(UUID.randomUUID()),
                metrica(UUID.randomUUID(), "10"));

        // mediana = 10 (el que no vendió no cuenta); la mitad = 5
        assertThat(servicio.esBajaRotacion(metrica, todas)).isFalse();
    }

    @Test
    @DisplayName("Sin otros productos con ventas no hay con qué comparar y no rota poco")
    void esBajaRotacionShouldBeFalseWithoutOtherProductsToCompare() {
        MetricaProducto metrica = metrica(PRODUCTO, "3");

        assertThat(servicio.esBajaRotacion(metrica, List.of(metrica, MetricaProducto.sinVentas(OTRO_PRODUCTO))))
                .isFalse();
    }

    // --- act ---
    private Map<UUID, MetricaProducto> whenCalcular(BloqueVentas ventas) {
        return servicio.calcular(List.of(ventas), HOY, PERIODO);
    }

    // --- helpers ---
    private static BloqueVentas ventas(Registro... filas) {
        return AvisoTestData.ventas(filas);
    }

    private static Registro venta(UUID producto, Object cantidad, Object precio, String fecha) {
        return AvisoTestData.venta(producto.toString(), cantidad, precio, fecha, null);
    }

    private static MetricaProducto metrica(UUID productoId, String unidades) {
        BigDecimal cantidad = new BigDecimal(unidades);
        return new MetricaProducto(productoId, cantidad, cantidad, null, HOY);
    }
}
