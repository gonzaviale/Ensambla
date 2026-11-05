package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.promocion.CanalPromocion;
import com.ensambla.backend.domain.model.promocion.Promocion;

import java.util.List;

/** Las promociones que conviene hacer y los canales donde se pueden compartir. */
public record SugerirPromocionesResponse(List<PromocionItem> promociones, List<CanalItem> canales) {

    public static SugerirPromocionesResponse de(List<Promocion> promociones, List<CanalPromocion> canales) {
        return new SugerirPromocionesResponse(
                promociones.stream().map(PromocionItem::de).toList(),
                canales.stream().map(CanalItem::de).toList());
    }
}
