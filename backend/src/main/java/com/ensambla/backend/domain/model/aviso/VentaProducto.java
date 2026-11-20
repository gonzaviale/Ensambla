package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.ValorDeRegistro;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Una fila de la tabla de ventas leída por sus claves semánticas. {@code precio} y {@code cliente} pueden faltar.
 *
 * @param productoId id del registro de inventario que se vendió
 * @param precio     precio unitario; {@code null} si la fila no lo tiene o no es positivo
 * @param cliente    quien se llevó el producto, tal como quedó cargado; {@code null} si no se anotó
 */
public record VentaProducto(
        RegistroId productoId, BigDecimal cantidad, BigDecimal precio, LocalDate fecha, String cliente) {

    static final String CLAVE_PRODUCTO = "product";
    static final String CLAVE_CANTIDAD = "quantity";
    static final String CLAVE_PRECIO = "price";
    static final String CLAVE_FECHA = "date";
    static final String CLAVE_CLIENTE = "customer";

    /** La venta de la fila, o vacío si no se puede leer: sin producto válido, sin cantidad positiva o sin fecha válida. */
    public static Optional<VentaProducto> desde(BloqueVentas ventas, Registro registro) {
        VentaProducto venta = new VentaProducto(
                producto(leer(ventas, registro, CLAVE_PRODUCTO)),
                leer(ventas, registro, CLAVE_CANTIDAD).comoNumeroPositivo(),
                leer(ventas, registro, CLAVE_PRECIO).comoNumeroPositivo(),
                leer(ventas, registro, CLAVE_FECHA).comoFecha(),
                leer(ventas, registro, CLAVE_CLIENTE).comoTexto());
        return Optional.of(venta).filter(VentaProducto::sePuedeContar);
    }

    private boolean sePuedeContar() {
        return productoId != null && cantidad != null && fecha != null;
    }

    private static ValorDeRegistro leer(BloqueVentas ventas, Registro registro, String clave) {
        return ValorDeRegistro.de(ventas.valorDe(registro, clave));
    }

    /** La conexión al producto se guarda como el id (texto) del registro de inventario. */
    private static RegistroId producto(ValorDeRegistro valor) {
        if (!valor.tieneTexto()) {
            return null;
        }
        try {
            return RegistroId.de(valor.comoTexto());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
