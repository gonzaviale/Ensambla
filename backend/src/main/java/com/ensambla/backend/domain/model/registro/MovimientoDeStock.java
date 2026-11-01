package com.ensambla.backend.domain.model.registro;

import java.math.BigDecimal;

/**
 * Lo que una venta, una salida o una entrada movió del stock de un producto. Una salida nunca deja el stock en
 * negativo: se descuenta hasta 0 y queda anotado que no alcanzó, como en el taller.
 *
 * @param producto  cómo se llama el producto
 * @param antes     el stock que tenía
 * @param despues   el stock que le quedó
 * @param pedido    las unidades que se vendieron, salieron o entraron
 * @param noAlcanzo si era una salida de más unidades de las que había
 */
public record MovimientoDeStock(String producto, BigDecimal antes, BigDecimal despues, BigDecimal pedido,
                                boolean noAlcanzo) {

    /** La clave del campo que vincula un movimiento con su producto. */
    public static final String CLAVE_PRODUCTO = "product";
    /** La clave del campo con las unidades: la cantidad de un movimiento y el stock de un producto. */
    public static final String CLAVE_CANTIDAD = "quantity";

    /** @param direccion +1 si el movimiento suma stock (entradas), -1 si lo descuenta (ventas y salidas) */
    public static MovimientoDeStock aplicar(String producto, BigDecimal habia, BigDecimal pedido, int direccion) {
        if (direccion > 0) {
            return new MovimientoDeStock(producto, habia, habia.add(pedido), pedido, false);
        }
        boolean noAlcanzo = pedido.compareTo(habia) > 0;
        return new MovimientoDeStock(producto, habia, habia.subtract(pedido).max(BigDecimal.ZERO), pedido, noAlcanzo);
    }
}
