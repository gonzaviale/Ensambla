package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Gasto;
import com.ensambla.backend.domain.model.objetivo.MedidorMiniObjetivo.Producto;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo único que Sami le cuenta a la IA del negocio para armar un objetivo: totales, productos con su precio y su
 * movimiento, gastos por categoría y los precios de la zona. Nada que identifique a una persona (ni el nombre del
 * negocio, ni clientes, ni contactos) y nada crudo del cuaderno.
 *
 * @param margen vacío si no hay costos con qué calcularlo
 */
public record ContextoObjetivoIa(
        boolean hayVentas,
        BigDecimal resultado,
        BigDecimal margen,
        BigDecimal ventasSemana,
        List<ProductoIa> productos,
        List<GastoIa> gastos,
        List<ProductoZonaIa> zona) {

    /** Cuántos productos de cada lista se le mandan a la IA, como mucho. */
    public static final int MAXIMO_POR_LISTA = 15;

    public record ProductoIa(String nombre, BigDecimal precio, BigDecimal stock, BigDecimal vendidas30Dias) {
    }

    public record GastoIa(String categoria, BigDecimal total) {
    }

    public record ProductoZonaIa(
            String nombre, int comercios, BigDecimal precioMinimo, BigDecimal precioPromedio,
            BigDecimal precioMaximo) {
    }

    public static ContextoObjetivoIa de(MedidorMiniObjetivo medidor, AnalisisZona zona) {
        return new ContextoObjetivoIa(
                medidor.hayVentas(),
                medidor.resultado(),
                medidor.margen().orElse(null),
                medidor.ventasSemana(),
                medidor.productos().stream().limit(MAXIMO_POR_LISTA).map(producto -> producto(medidor, producto))
                        .toList(),
                medidor.gastos().stream().limit(MAXIMO_POR_LISTA).map(ContextoObjetivoIa::gasto).toList(),
                zona.masVendidos().stream().limit(MAXIMO_POR_LISTA).map(ContextoObjetivoIa::productoDeZona).toList());
    }

    /** Qué fuentes tienen datos y cuáles no, en un orden fijo. */
    public List<DatoUsado> datos() {
        return List.of(
                new DatoUsado(FuenteDato.VENTAS, hayVentas),
                new DatoUsado(FuenteDato.MARGEN, margen != null),
                new DatoUsado(FuenteDato.INVENTARIO, !productos.isEmpty()),
                new DatoUsado(FuenteDato.GASTOS, !gastos.isEmpty()),
                new DatoUsado(FuenteDato.ZONA, !zona.isEmpty()));
    }

    private static ProductoIa producto(MedidorMiniObjetivo medidor, Producto producto) {
        return new ProductoIa(producto.nombre(), producto.precio(), producto.stock(),
                medidor.unidades(producto.id(), MedidorMiniObjetivo.DIAS_MES));
    }

    private static GastoIa gasto(Gasto gasto) {
        return new GastoIa(gasto.categoria(), gasto.total());
    }

    private static ProductoZonaIa productoDeZona(AgregadoZonaProducto producto) {
        return new ProductoZonaIa(producto.nombre(), producto.comerciosVenden(), producto.precioMinimo(),
                producto.precioPromedio(), producto.precioMaximo());
    }
}
