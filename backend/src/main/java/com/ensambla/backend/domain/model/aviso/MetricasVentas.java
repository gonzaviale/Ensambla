package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Métricas de venta por producto, calculadas con las filas de las tablas de ventas del negocio. Es lógica pura: recibe
 * las tablas ya cargadas. El período y el factor de baja rotación se configuran con {@code ensambla.avisos.periodo-dias}
 * y {@code ensambla.avisos.baja-rotacion-factor}.
 *
 * @param periodoDias        días hacia atrás desde hoy que cuentan como el período
 * @param factorBajaRotacion un producto rota poco si vendió este factor de la mediana de los demás, o menos
 */
public record MetricasVentas(int periodoDias, BigDecimal factorBajaRotacion) {

    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    /**
     * Métricas de cada producto con al menos una venta válida hasta {@code hoy}. Se ignoran las filas sin producto,
     * cantidad o fecha válidos y las ventas con fecha futura.
     */
    public Map<RegistroId, MetricaProducto> calcular(List<BloqueVentas> ventas, LocalDate hoy) {
        Map<RegistroId, List<VentaProducto>> porProducto = new LinkedHashMap<>();
        ventasValidas(ventas, hoy).forEach(venta ->
                porProducto.computeIfAbsent(venta.productoId(), id -> new ArrayList<>()).add(venta));
        Map<RegistroId, MetricaProducto> metricas = new LinkedHashMap<>();
        porProducto.forEach((productoId, filas) -> metricas.put(productoId, metricaDe(productoId, filas, hoy)));
        return metricas;
    }

    /** Todas las ventas válidas hasta hoy, para que otros cálculos (como las canastas) lean las filas igual. */
    public List<VentaProducto> ventasValidas(List<BloqueVentas> ventas, LocalDate hoy) {
        return ventas.stream()
                .flatMap(bloque -> bloque.registros().stream()
                        .flatMap(registro -> VentaProducto.desde(bloque, registro).stream()))
                .filter(venta -> !venta.fecha().isAfter(hoy))
                .toList();
    }

    /**
     * Un producto rota poco si no vendió nada en el período o si vendió {@code factor} veces la mediana de los demás
     * productos con ventas, o menos. Sin otros productos con ventas no hay con qué comparar: sólo cuenta si no vendió
     * nada.
     */
    public boolean esBajaRotacion(MetricaProducto metrica, Collection<MetricaProducto> todas) {
        if (metrica.unidadesPeriodo().signum() == 0) {
            return true;
        }
        List<BigDecimal> demas = todas.stream()
                .filter(otra -> !otra.productoId().equals(metrica.productoId()))
                .map(MetricaProducto::unidadesPeriodo)
                .filter(unidades -> unidades.signum() > 0)
                .sorted()
                .toList();
        if (demas.isEmpty()) {
            return false;
        }
        return metrica.unidadesPeriodo().compareTo(mediana(demas).multiply(factorBajaRotacion)) <= 0;
    }

    private MetricaProducto metricaDe(RegistroId productoId, List<VentaProducto> filas, LocalDate hoy) {
        LocalDate desde = hoy.minusDays(periodoDias);
        BigDecimal enPeriodo = suma(filas.stream()
                .filter(venta -> venta.fecha().isAfter(desde))
                .map(VentaProducto::cantidad));
        BigDecimal total = suma(filas.stream().map(VentaProducto::cantidad));
        LocalDate primera = filas.stream().map(VentaProducto::fecha).min(LocalDate::compareTo).orElse(hoy);
        LocalDate ultima = filas.stream().map(VentaProducto::fecha).max(LocalDate::compareTo).orElse(null);
        return new MetricaProducto(productoId, enPeriodo, promedioPorPeriodo(total, primera, hoy),
                precioPromedio(filas), ultima);
    }

    /** Unidades por período desde la primera venta; con menos de un período de historia cuenta como uno. */
    private BigDecimal promedioPorPeriodo(BigDecimal total, LocalDate primera, LocalDate hoy) {
        BigDecimal dias = BigDecimal.valueOf(ChronoUnit.DAYS.between(primera, hoy) + 1);
        BigDecimal periodos = dias.divide(BigDecimal.valueOf(periodoDias), 4, RoundingMode.HALF_UP)
                .max(BigDecimal.ONE);
        return total.divide(periodos, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal precioPromedio(List<VentaProducto> filas) {
        List<VentaProducto> conPrecio = filas.stream().filter(venta -> venta.precio() != null).toList();
        BigDecimal unidades = suma(conPrecio.stream().map(VentaProducto::cantidad));
        if (unidades.signum() == 0) {
            return null;
        }
        BigDecimal importe = suma(conPrecio.stream().map(venta -> venta.precio().multiply(venta.cantidad())));
        return importe.divide(unidades, 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal suma(Stream<BigDecimal> valores) {
        return valores.reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal mediana(List<BigDecimal> ordenados) {
        int medio = ordenados.size() / 2;
        return ordenados.size() % 2 == 1
                ? ordenados.get(medio)
                : ordenados.get(medio - 1).add(ordenados.get(medio)).divide(DOS);
    }
}
