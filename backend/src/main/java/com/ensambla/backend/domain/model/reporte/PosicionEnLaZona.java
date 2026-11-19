package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoDesalineado;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/** Dónde cae el precio de un producto frente al rango de precios de la zona, y cuánto se aleja si queda afuera. */
enum PosicionEnLaZona {

    DEBAJO {
        @Override
        Optional<ProductoDesalineado> desalineado(ProductoDelResumen producto, AgregadoZonaProducto rango) {
            return Optional.of(alejado(producto, rango, rango.precioMinimo()));
        }
    },
    DENTRO {
        @Override
        Optional<ProductoDesalineado> desalineado(ProductoDelResumen producto, AgregadoZonaProducto rango) {
            return Optional.empty();
        }
    },
    ENCIMA {
        @Override
        Optional<ProductoDesalineado> desalineado(ProductoDelResumen producto, AgregadoZonaProducto rango) {
            return Optional.of(alejado(producto, rango, rango.precioMaximo()));
        }
    };

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    static PosicionEnLaZona de(BigDecimal precio, AgregadoZonaProducto rango) {
        if (precio.compareTo(rango.precioMinimo()) < 0) {
            return DEBAJO;
        }
        if (precio.compareTo(rango.precioMaximo()) > 0) {
            return ENCIMA;
        }
        return DENTRO;
    }

    /** El producto si queda fuera del rango, con el porcentaje que se aleja del borde que pasó. */
    abstract Optional<ProductoDesalineado> desalineado(ProductoDelResumen producto, AgregadoZonaProducto rango);

    private static ProductoDesalineado alejado(ProductoDelResumen producto, AgregadoZonaProducto rango, BigDecimal borde) {
        BigDecimal porcentaje = producto.precio().subtract(borde).multiply(CIEN).divide(borde, 0, RoundingMode.HALF_UP);
        return new ProductoDesalineado(producto.nombre(), producto.precio(), rango.precioMinimo(), rango.precioMaximo(),
                porcentaje);
    }
}
