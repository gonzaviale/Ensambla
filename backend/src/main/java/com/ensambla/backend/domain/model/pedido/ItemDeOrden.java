package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

/**
 * Un ítem de la orden: lo que el cliente confirmó, con el nombre y el precio del momento, así la orden no cambia si el
 * comercio después edita su inventario.
 *
 * @param productoId     la fila del inventario
 * @param nombre         el producto
 * @param cantidad       cuántas unidades
 * @param precioUnitario precio por unidad al confirmar
 */
public record ItemDeOrden(RegistroId productoId, String nombre, BigDecimal cantidad, BigDecimal precioUnitario) {

    public static ItemDeOrden de(LineaDeCompra linea) {
        return new ItemDeOrden(linea.producto().id(), linea.producto().nombre(), linea.cantidad(),
                linea.producto().precio());
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(cantidad);
    }
}
