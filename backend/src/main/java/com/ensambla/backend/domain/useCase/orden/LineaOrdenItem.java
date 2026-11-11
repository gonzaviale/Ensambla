package com.ensambla.backend.domain.useCase.orden;

import com.ensambla.backend.domain.model.pedido.ItemDeOrden;

import java.math.BigDecimal;

/** Un producto de la orden con la cantidad y el precio que confirmó el cliente. */
public record LineaOrdenItem(String nombre, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {

    public static LineaOrdenItem de(ItemDeOrden item) {
        return new LineaOrdenItem(item.nombre(), item.cantidad(), item.precioUnitario(), item.subtotal());
    }
}
