package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.Faltante;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio.ProductoParado;

import java.math.BigDecimal;

/** Una fila del inventario como la mira el resumen; {@code precio} y {@code costo} son {@code null} si no son positivos. */
record ProductoDelResumen(String nombre, BigDecimal cantidad, BigDecimal precio, BigDecimal costo, BigDecimal minimo) {

    private static final String SIN_NOMBRE = "Producto sin nombre";
    private static final BigDecimal DOS = BigDecimal.valueOf(2);

    static ProductoDelResumen de(BloqueInventario inventario, Registro registro) {
        return new ProductoDelResumen(
                leer(inventario, registro, "name").comoTextoO(SIN_NOMBRE),
                leer(inventario, registro, "quantity").comoNumero(),
                leer(inventario, registro, "price").comoNumeroPositivo(),
                leer(inventario, registro, "cost").comoNumeroPositivo(),
                leer(inventario, registro, "minimum").comoNumero());
    }

    private static ValorDeRegistro leer(BloqueInventario inventario, Registro registro, String clave) {
        return ValorDeRegistro.de(inventario.valorDe(registro, clave));
    }

    boolean hayStock() {
        return cantidad != null && cantidad.signum() > 0;
    }

    /** Agotado, o en su mínimo o por debajo cuando definió uno. */
    boolean estaEnFalta() {
        return cantidad != null && (cantidad.signum() <= 0 || estaEnElMinimoOPorDebajo());
    }

    Faltante faltante() {
        return new Faltante(nombre, cantidad, minimo, cantidadAPedir());
    }

    ProductoParado parado() {
        return new ProductoParado(nombre, cantidad, capitalInmovilizado());
    }

    BigDecimal valorACosto() {
        return hayStock() && costo != null ? cantidad.multiply(costo) : BigDecimal.ZERO;
    }

    BigDecimal valorAPrecio() {
        return hayStock() && precio != null ? cantidad.multiply(precio) : BigDecimal.ZERO;
    }

    private BigDecimal capitalInmovilizado() {
        return costo == null ? null : cantidad.multiply(costo);
    }

    /** Lo que falta para llegar al doble del mínimo, al menos una unidad; {@code null} si no definió mínimo. */
    private BigDecimal cantidadAPedir() {
        return tieneMinimo() ? minimo.multiply(DOS).subtract(cantidad).max(BigDecimal.ONE) : null;
    }

    private boolean estaEnElMinimoOPorDebajo() {
        return tieneMinimo() && cantidad.compareTo(minimo) <= 0;
    }

    private boolean tieneMinimo() {
        return minimo != null && minimo.signum() > 0;
    }
}
