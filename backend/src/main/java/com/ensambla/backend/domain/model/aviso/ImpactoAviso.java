package com.ensambla.backend.domain.model.aviso;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Qué pasó con las ventas de un producto antes y después de aplicar un aviso: unidades e ingreso por día en los
 * {@code ventanaDias} anteriores al día en que se aplicó y en los {@code ventanaDias} que empiezan ese día. Es una
 * comparación, no una causa: otras cosas también cambian las ventas.
 */
public record ImpactoAviso(
        int ventanaDias,
        BigDecimal unidadesPorDiaAntes,
        BigDecimal unidadesPorDiaDespues,
        BigDecimal ingresoPorDiaAntes,
        BigDecimal ingresoPorDiaDespues) {

    private static final BigDecimal DIAS_POR_SEMANA = BigDecimal.valueOf(7);

    static final String CLAVE = "impacto";

    /**
     * El impacto de las ventas de un producto, o vacío si la ventana posterior todavía no terminó: el día de hoy no
     * cuenta porque sus ventas siguen entrando.
     */
    public static Optional<ImpactoAviso> medir(
            List<VentaProducto> ventas, LocalDate aplicadoEl, LocalDate hoy, int ventanaDias) {
        if (aplicadoEl.plusDays(ventanaDias).isAfter(hoy)) {
            return Optional.empty();
        }
        LocalDate desde = aplicadoEl.minusDays(ventanaDias);
        LocalDate hasta = aplicadoEl.plusDays(ventanaDias);
        List<VentaProducto> antes = ventas.stream()
                .filter(venta -> !venta.fecha().isBefore(desde) && venta.fecha().isBefore(aplicadoEl)).toList();
        List<VentaProducto> despues = ventas.stream()
                .filter(venta -> !venta.fecha().isBefore(aplicadoEl) && venta.fecha().isBefore(hasta)).toList();
        return Optional.of(new ImpactoAviso(ventanaDias,
                porDia(unidades(antes), ventanaDias), porDia(unidades(despues), ventanaDias),
                porDia(ingreso(antes), ventanaDias), porDia(ingreso(despues), ventanaDias)));
    }

    /** Lo que el impacto guardó en los datos del aviso, o vacío si todavía no se midió. */
    public static Optional<ImpactoAviso> de(Map<String, Object> datos) {
        if (datos == null || !(datos.get(CLAVE) instanceof Map<?, ?> medido)) {
            return Optional.empty();
        }
        return Optional.of(new ImpactoAviso(
                numero(medido.get("ventanaDias")).intValue(),
                numero(medido.get("unidadesPorDiaAntes")),
                numero(medido.get("unidadesPorDiaDespues")),
                numero(medido.get("ingresoPorDiaAntes")),
                numero(medido.get("ingresoPorDiaDespues"))));
    }

    /** Lo que cambió el ingreso semanal: positivo si se vende más después del aviso. */
    public BigDecimal deltaIngresoSemanal() {
        return ingresoPorDiaDespues.subtract(ingresoPorDiaAntes).multiply(DIAS_POR_SEMANA)
                .setScale(2, RoundingMode.HALF_UP);
    }

    Map<String, Object> aMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("ventanaDias", ventanaDias);
        mapa.put("unidadesPorDiaAntes", unidadesPorDiaAntes);
        mapa.put("unidadesPorDiaDespues", unidadesPorDiaDespues);
        mapa.put("ingresoPorDiaAntes", ingresoPorDiaAntes);
        mapa.put("ingresoPorDiaDespues", ingresoPorDiaDespues);
        return mapa;
    }

    private static BigDecimal unidades(List<VentaProducto> ventas) {
        return ventas.stream().map(VentaProducto::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sólo cuentan las ventas con precio: sin él no se sabe cuánto ingresó. */
    private static BigDecimal ingreso(List<VentaProducto> ventas) {
        return ventas.stream()
                .filter(venta -> venta.precio() != null)
                .map(venta -> venta.precio().multiply(venta.cantidad()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal porDia(BigDecimal total, int dias) {
        return total.divide(BigDecimal.valueOf(dias), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal numero(Object valor) {
        return new BigDecimal(String.valueOf(valor));
    }
}
