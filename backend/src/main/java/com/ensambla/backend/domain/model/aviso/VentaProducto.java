package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
        RegistroId producto = producto(ventas.valorDe(registro, CLAVE_PRODUCTO));
        BigDecimal cantidad = numero(ventas.valorDe(registro, CLAVE_CANTIDAD));
        LocalDate fecha = fecha(ventas.valorDe(registro, CLAVE_FECHA));
        if (producto == null || cantidad == null || cantidad.signum() <= 0 || fecha == null) {
            return Optional.empty();
        }
        BigDecimal precio = numero(ventas.valorDe(registro, CLAVE_PRECIO));
        return Optional.of(new VentaProducto(
                producto, cantidad, precio != null && precio.signum() > 0 ? precio : null, fecha,
                cliente(ventas.valorDe(registro, CLAVE_CLIENTE))));
    }

    /** La conexión al producto se guarda como el id (texto) del registro de inventario. */
    private static RegistroId producto(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        try {
            return RegistroId.de(texto.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }

    private static LocalDate fecha(Object valor) {
        if (!(valor instanceof String texto)) {
            return null;
        }
        try {
            return LocalDate.parse(texto.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String cliente(Object valor) {
        return valor instanceof String texto && !texto.isBlank() ? texto.trim() : null;
    }
}
