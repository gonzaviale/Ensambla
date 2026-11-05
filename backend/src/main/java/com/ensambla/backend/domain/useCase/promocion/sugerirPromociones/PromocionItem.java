package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.promocion.Promocion;

import java.util.List;

public record PromocionItem(String clave, String titulo, String gancho, List<ProductoPromocionadoItem> productos) {

    public static PromocionItem de(Promocion promocion) {
        return new PromocionItem(
                promocion.clave(),
                promocion.titulo(),
                promocion.gancho(),
                promocion.productos().stream().map(ProductoPromocionadoItem::de).toList());
    }
}
