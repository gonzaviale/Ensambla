package com.ensambla.backend.domain.useCase.reporte.obtenerMetricas;

import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.VentaDelDia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Las métricas detalladas de un sistema en un período: lo vendido y lo que se ganó, las ventas día por día, los
 * productos, el stock, los clientes y los gastos. Son las mismas cuentas que el resumen en PDF.
 *
 * @param hayVentas si el sistema tiene al menos una venta anotada hasta hoy, de cualquier fecha
 * @param porDia    lo que entró cada día del período, del más viejo a hoy
 * @param parados   productos con stock que no se vendieron en las últimas dos semanas; vacío si todavía no hay dos
 *                  semanas de ventas anotadas
 * @param gastos    {@code null} si el sistema no tiene una tabla de gastos con importe
 */
public record ObtenerMetricasResponse(
        String sistema,
        PeriodoMetricas periodo,
        LocalDate desde,
        LocalDate hasta,
        boolean hayVentas,
        Ventas ventas,
        List<VentaDelDia> porDia,
        List<ProductoVendido> masVendidos,
        List<Parado> parados,
        Stock stock,
        Clientes clientes,
        Gastos gastos) {

    /**
     * @param sinPrecio          ventas del período sin precio: cuentan como unidades pero no suman plata
     * @param ingresoPorDia      los ingresos repartidos en todos los días del período
     * @param ingresosAnteriores lo que entró en el período con el que se compara: el mismo día de la semana pasada
     *                           para hoy, y los 7 o 30 días anteriores para la semana y el mes
     * @param gananciaEstimada   ventas menos el costo actual de lo vendido; {@code null} si ningún producto tiene costo
     */
    public record Ventas(
            BigDecimal ingresos,
            BigDecimal unidades,
            long cantidad,
            long sinPrecio,
            BigDecimal ingresoPorDia,
            BigDecimal ingresosAnteriores,
            BigDecimal gananciaEstimada) {
    }

    public record ProductoVendido(String nombre, BigDecimal unidades, BigDecimal ingresos, BigDecimal ganancia) {
    }

    public record Parado(String nombre, BigDecimal cantidad, BigDecimal capital) {
    }

    public record Faltante(String nombre, BigDecimal cantidad, BigDecimal minimo, BigDecimal pedir) {
    }

    public record Stock(
            long productos,
            long totalFaltantes,
            List<Faltante> faltantes,
            BigDecimal valorACosto,
            BigDecimal valorAPrecio) {
    }

    public record Cliente(String nombre, long compras, BigDecimal ingresos) {
    }

    /** {@code fiado} es lo vendido a fiado en el período, no lo que todavía se debe. */
    public record Clientes(List<Cliente> top, BigDecimal fiado, long ventasFiadas) {
    }

    public record GastoPorCategoria(String categoria, BigDecimal total) {
    }

    /** {@code resultado} son los ingresos del período menos los gastos. */
    public record Gastos(BigDecimal total, BigDecimal resultado, List<GastoPorCategoria> porCategoria) {
    }

    /**
     * Arma las métricas con los resúmenes ya calculados.
     *
     * @param resumen            resumen del período pedido
     * @param quincena           resumen de las últimas dos semanas, para los productos parados
     * @param ingresosAnteriores lo que entró en el período con el que se compara
     * @param puedeListarParados si hay al menos dos semanas de ventas anotadas
     */
    public static ObtenerMetricasResponse de(
            PeriodoMetricas periodo,
            boolean hayVentas,
            ResumenNegocio resumen,
            ResumenNegocio quincena,
            BigDecimal ingresosAnteriores,
            long sinPrecio,
            List<VentaDelDia> porDia,
            boolean puedeListarParados) {
        ResumenNegocio.Ventas ventas = resumen.ventas();
        ResumenNegocio.Stock stock = resumen.stock();
        ResumenNegocio.Clientes clientes = resumen.clientes();
        return new ObtenerMetricasResponse(
                resumen.sistema(),
                periodo,
                resumen.periodo().desde(),
                resumen.periodo().hasta(),
                hayVentas,
                new Ventas(ventas.ingresos(), ventas.unidades(), ventas.cantidad(), sinPrecio, ventas.ingresoPorDia(),
                        ingresosAnteriores, ventas.gananciaEstimada()),
                porDia,
                resumen.masVendidos().stream()
                        .map(producto -> new ProductoVendido(producto.nombre(), producto.unidades(),
                                producto.ingresos(), producto.ganancia()))
                        .toList(),
                puedeListarParados
                        ? quincena.sinMovimiento().stream()
                                .map(parado -> new Parado(parado.nombre(), parado.cantidad(), parado.capital()))
                                .toList()
                        : List.of(),
                new Stock(stock.productos(), stock.totalFaltantes(), stock.faltantes().stream()
                        .map(faltante -> new Faltante(faltante.nombre(), faltante.cantidad(), faltante.minimo(),
                                faltante.pedir()))
                        .toList(), stock.valorACosto(), stock.valorAPrecio()),
                new Clientes(clientes.top().stream()
                        .map(cliente -> new Cliente(cliente.nombre(), cliente.compras(), cliente.ingresos()))
                        .toList(), clientes.fiado(), clientes.ventasFiadas()),
                gastosDe(resumen.gastos()));
    }

    private static Gastos gastosDe(ResumenNegocio.Gastos gastos) {
        if (gastos == null) {
            return null;
        }
        return new Gastos(gastos.total(), gastos.resultado(), gastos.porCategoria().stream()
                .map(categoria -> new GastoPorCategoria(categoria.categoria(), categoria.total()))
                .toList());
    }
}
