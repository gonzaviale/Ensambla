package com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio;

import com.ensambla.backend.domain.model.reporte.ResumenNegocio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Las métricas que se muestran en el inicio: lo vendido hoy, lo que más salió, lo que falta y lo que está parado.
 * Cada parte viene vacía ({@code null} o lista vacía) cuando el sistema no tiene datos para calcularla, para que el
 * inicio no muestre ceros que no son ciertos.
 *
 * @param sistema           nombre del sistema (cuaderno) del que son los números
 * @param hoy               el día de hoy, en hora argentina
 * @param hayVentas         si el sistema tiene al menos una venta anotada hasta hoy
 * @param hayProductos      si el sistema tiene productos en su inventario
 * @param ventasDeHoy       lo vendido hoy
 * @param ultimoDiaConVentas el último día en que se anotó una venta; {@code null} si nunca se vendió
 * @param masVendido        el producto que más unidades vendió en los últimos 30 días; {@code null} si no vendió nada
 * @param faltantes         productos agotados o bajo su mínimo
 * @param parados           productos con stock que no se vendieron en las últimas dos semanas; vacío si el sistema
 *                          todavía no tiene dos semanas de ventas anotadas
 */
public record ObtenerResumenInicioResponse(
        String sistema,
        LocalDate hoy,
        boolean hayVentas,
        boolean hayProductos,
        VentasDeHoy ventasDeHoy,
        UltimoDiaConVentas ultimoDiaConVentas,
        MasVendido masVendido,
        Faltantes faltantes,
        List<Parado> parados) {

    /** Máximo de productos que se listan como faltantes o parados: el inicio muestra un adelanto, no la tabla entera. */
    static final int MAXIMO_PRODUCTOS = 3;

    /**
     * @param ingresos            lo que entró hoy por las ventas con precio
     * @param unidades            unidades vendidas hoy
     * @param cantidad            filas de venta de hoy (cada una es un producto, no un cliente)
     * @param sinPrecio           cuántas de esas filas no tienen precio y por eso no suman en {@code ingresos}
     * @param ingresosSemanaPasada lo que entró el mismo día de la semana pasada; sirve para comparar
     */
    public record VentasDeHoy(
            BigDecimal ingresos,
            BigDecimal unidades,
            long cantidad,
            long sinPrecio,
            BigDecimal ingresosSemanaPasada) {
    }

    public record UltimoDiaConVentas(LocalDate fecha, BigDecimal ingresos) {
    }

    /** {@code ganancia} es {@code null} si el producto no tiene costo cargado. */
    public record MasVendido(String nombre, BigDecimal unidades, BigDecimal ingresos, BigDecimal ganancia) {
    }

    public record Faltante(String nombre, BigDecimal cantidad, BigDecimal minimo, BigDecimal pedir) {
    }

    /** {@code total} cuenta todos los faltantes; {@code primeros} son los más urgentes, hasta tres. */
    public record Faltantes(long total, List<Faltante> primeros) {
    }

    /** {@code capital} es lo invertido en el stock parado; {@code null} si el producto no tiene costo cargado. */
    public record Parado(String nombre, BigDecimal cantidad, BigDecimal capital) {
    }

    /**
     * Arma las métricas con los resúmenes ya calculados.
     *
     * @param mes       resumen de los últimos 30 días
     * @param quincena  resumen de las últimas dos semanas
     * @param hoyResumen resumen de hoy
     * @param puedeListarParados si hay al menos dos semanas de ventas anotadas, que es lo que hace cierto que un
     *                           producto "no se vendió" en ese tiempo
     */
    public static ObtenerResumenInicioResponse de(
            LocalDate hoy,
            boolean hayVentas,
            ResumenNegocio mes,
            ResumenNegocio quincena,
            ResumenNegocio hoyResumen,
            BigDecimal ingresosSemanaPasada,
            long ventasDeHoySinPrecio,
            UltimoDiaConVentas ultimoDia,
            boolean puedeListarParados) {
        ResumenNegocio.Ventas ventas = hoyResumen.ventas();
        return new ObtenerResumenInicioResponse(
                mes.sistema(),
                hoy,
                hayVentas,
                mes.stock().productos() > 0,
                new VentasDeHoy(ventas.ingresos(), ventas.unidades(), ventas.cantidad(), ventasDeHoySinPrecio,
                        ingresosSemanaPasada),
                ultimoDia,
                mes.masVendidos().stream().findFirst()
                        .map(producto -> new MasVendido(producto.nombre(), producto.unidades(), producto.ingresos(),
                                producto.ganancia()))
                        .orElse(null),
                new Faltantes(mes.stock().totalFaltantes(), mes.stock().faltantes().stream()
                        .limit(MAXIMO_PRODUCTOS)
                        .map(faltante -> new Faltante(faltante.nombre(), faltante.cantidad(), faltante.minimo(),
                                faltante.pedir()))
                        .toList()),
                puedeListarParados
                        ? quincena.sinMovimiento().stream()
                                .limit(MAXIMO_PRODUCTOS)
                                .map(parado -> new Parado(parado.nombre(), parado.cantidad(), parado.capital()))
                                .toList()
                        : List.of());
    }
}
