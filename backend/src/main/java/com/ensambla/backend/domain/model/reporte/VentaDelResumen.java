package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.VentaProducto;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/** Una venta del proyecto y si quedó a fiado; las ventas sin precio cuentan en las unidades pero no en el ingreso. */
record VentaDelResumen(VentaProducto venta, boolean fiado) {

    BigDecimal ingreso() {
        return tienePrecio() ? venta.precio().multiply(venta.cantidad()) : BigDecimal.ZERO;
    }

    BigDecimal unidades() {
        return venta.cantidad();
    }

    LocalDate fecha() {
        return venta.fecha();
    }

    RegistroId productoId() {
        return venta.productoId();
    }

    String cliente() {
        return venta.cliente();
    }

    boolean esDeUnProducto() {
        return productoId() != null;
    }

    boolean tieneCliente() {
        return cliente() != null;
    }

    boolean estaEn(PeriodoResumen periodo) {
        return periodo.incluye(fecha());
    }

    /** Lo vendido menos el costo actual del producto; {@code null} si no tiene precio, producto o costo. */
    BigDecimal ganancia(Map<RegistroId, ProductoDelResumen> productos) {
        ProductoDelResumen producto = esDeUnProducto() ? productos.get(productoId()) : null;
        if (!tienePrecio() || producto == null || producto.costo() == null) {
            return null;
        }
        return venta.precio().subtract(producto.costo()).multiply(venta.cantidad());
    }

    private boolean tienePrecio() {
        return venta.precio() != null;
    }
}
