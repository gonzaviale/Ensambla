package com.ensambla.backend.domain.model.pedido;

import java.math.BigDecimal;
import java.util.List;

/**
 * El resultado de buscar en el stock del comercio lo que pidió el cliente.
 *
 * @param lineas    lo que se pudo agregar a la lista de compra
 * @param faltantes lo que el comercio no tiene
 */
public record ArmadoDeLista(List<LineaDeCompra> lineas, List<ProductoFaltante> faltantes) {

    public ArmadoDeLista {
        lineas = List.copyOf(lineas);
        faltantes = List.copyOf(faltantes);
    }

    public BigDecimal total() {
        return total(lineas);
    }

    /** Lo que suma una lista de compra. */
    public static BigDecimal total(List<LineaDeCompra> lineas) {
        return lineas.stream().map(LineaDeCompra::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
