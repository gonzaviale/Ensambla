package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.aviso.BloqueInventario;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Un producto del inventario del comercio que se puede pedir: tiene nombre, precio y stock. Todo sale de la fila del
 * inventario ({@code name}, {@code price} y {@code quantity}); lo que no está cargado no se ofrece.
 *
 * @param id     la fila del inventario
 * @param nombre como lo escribió el comercio
 * @param precio precio por unidad
 * @param stock  unidades disponibles
 */
public record ProductoEnStock(RegistroId id, String nombre, BigDecimal precio, BigDecimal stock) {

    private static final String CLAVE_NOMBRE = "name";
    private static final String CLAVE_PRECIO = "price";
    private static final String CLAVE_STOCK = "quantity";

    /** @return el producto, o vacío si le falta el nombre o el precio, o no tiene stock */
    public static Optional<ProductoEnStock> de(BloqueInventario inventario, Registro registro) {
        Object nombre = inventario.valorDe(registro, CLAVE_NOMBRE);
        BigDecimal precio = numero(inventario.valorDe(registro, CLAVE_PRECIO));
        BigDecimal stock = numero(inventario.valorDe(registro, CLAVE_STOCK));
        boolean pedible = nombre instanceof String texto && !texto.isBlank()
                && precio != null && precio.signum() > 0 && stock != null && stock.signum() > 0;
        return pedible
                ? Optional.of(new ProductoEnStock(registro.getId(), ((String) nombre).strip(), precio, stock))
                : Optional.empty();
    }

    private static BigDecimal numero(Object valor) {
        return valor instanceof Number numero ? new BigDecimal(numero.toString()) : null;
    }
}
