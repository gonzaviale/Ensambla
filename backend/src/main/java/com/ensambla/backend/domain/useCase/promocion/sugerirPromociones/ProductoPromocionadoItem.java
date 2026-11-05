package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.promocion.ProductoPromocionado;

import java.math.BigDecimal;

public record ProductoPromocionadoItem(String nombre, BigDecimal precio) {

    public static ProductoPromocionadoItem de(ProductoPromocionado producto) {
        return new ProductoPromocionadoItem(producto.nombre(), producto.precio());
    }
}
