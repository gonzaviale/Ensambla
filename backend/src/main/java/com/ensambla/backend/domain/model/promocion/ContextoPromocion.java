package com.ensambla.backend.domain.model.promocion;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.aviso.MetricaProducto;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Lo que las reglas de promoción saben del negocio: los productos que se pueden promocionar y cómo se vendieron.
 * {@code metricas} trae sólo los productos con al menos una venta anotada.
 */
public record ContextoPromocion(List<ProductoDeInventario> productos, Map<RegistroId, MetricaProducto> metricas) {

    public static ContextoPromocion de(DatosDeProyecto datos, Map<RegistroId, MetricaProducto> metricas) {
        List<ProductoDeInventario> productos = datos.inventarios().stream()
                .flatMap(ContextoPromocion::productosDe)
                .toList();
        return new ContextoPromocion(productos, metricas);
    }

    /** Unidades vendidas en el último período; cero si el producto no tiene ventas. */
    public BigDecimal unidadesVendidas(ProductoDeInventario producto) {
        return Optional.ofNullable(metricas.get(producto.id()))
                .map(MetricaProducto::unidadesPeriodo)
                .orElse(BigDecimal.ZERO);
    }

    /** Sin ninguna venta anotada no se puede saber qué se mueve y qué está parado. */
    public boolean hayVentasAnotadas() {
        return !metricas.isEmpty();
    }

    private static Stream<ProductoDeInventario> productosDe(BloqueInventario inventario) {
        return inventario.registros().stream()
                .map(registro -> ProductoDeInventario.de(inventario, registro))
                .flatMap(Optional::stream);
    }
}
