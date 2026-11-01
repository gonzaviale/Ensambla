package com.ensambla.backend.domain.useCase.zona;

import com.ensambla.backend.domain.model.common.Huella;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.useCase.plan.AccesoContenidoItem;

import java.math.BigDecimal;
import java.util.List;

public record ZonaItem(String nombre, long comercios, List<ProductoItem> productos, AccesoContenidoItem acceso) {

    public ZonaItem(String nombre, long comercios, List<ProductoItem> productos) {
        this(nombre, comercios, productos, null);
    }

    public static ZonaItem de(AnalisisZona analisis) {
        return new ZonaItem(
                analisis.nombre(),
                analisis.comercios(),
                analisis.productos().stream().map(ProductoItem::de).toList());
    }

    /** Una clave estable por producto: es lo que se cuenta contra el cupo del plan gratuito. */
    public List<String> clavesDeAcceso() {
        return productos.stream().map(this::clave).toList();
    }

    /** La zona con sólo los productos que el plan deja ver, junto con el detalle del acceso. */
    public ZonaItem filtrada(AccesoContenidoItem acceso) {
        return new ZonaItem(nombre, comercios,
                productos.stream().filter(producto -> acceso.autorizadas().contains(clave(producto))).toList(), acceso);
    }

    private String clave(ProductoItem producto) {
        return "zona:" + Huella.de(nombre + "|" + producto.nombre());
    }

    public record ProductoItem(
            String nombre,
            int comercios,
            BigDecimal precioMinimo,
            BigDecimal precioPromedio,
            BigDecimal precioMaximo,
            int comerciosVenden,
            BigDecimal unidadesPorSemana) {

        static ProductoItem de(AgregadoZonaProducto producto) {
            return new ProductoItem(
                    producto.nombre(),
                    producto.comercios(),
                    producto.precioMinimo(),
                    producto.precioPromedio(),
                    producto.precioMaximo(),
                    producto.comerciosVenden(),
                    producto.unidadesSemana());
        }
    }
}
