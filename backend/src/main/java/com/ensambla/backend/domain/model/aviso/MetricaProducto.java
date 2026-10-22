package com.ensambla.backend.domain.model.aviso;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cómo se vendió un producto.
 *
 * @param unidadesPeriodo         unidades vendidas en el último período (por defecto, 30 días)
 * @param promedioPorPeriodo      unidades por período desde la primera venta que anotó el negocio
 * @param precioPromedioHistorico precio al que lo vendió, ponderado por cantidad; {@code null} si ninguna venta tiene precio
 * @param ultimaVenta             fecha de la última venta; {@code null} si nunca se vendió
 */
public record MetricaProducto(
        UUID productoId,
        BigDecimal unidadesPeriodo,
        BigDecimal promedioPorPeriodo,
        BigDecimal precioPromedioHistorico,
        LocalDate ultimaVenta) {

    /** Producto del que no hay ninguna venta anotada. */
    public static MetricaProducto sinVentas(UUID productoId) {
        return new MetricaProducto(productoId, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }
}
