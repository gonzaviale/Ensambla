package com.ensambla.backend.domain.useCase.promocion.sugerirPromociones;

import com.ensambla.backend.domain.model.promocion.CanalPromocion;

/** Un lugar donde se puede compartir una promoción. */
public record CanalItem(String clave, String nombre) {

    public static CanalItem de(CanalPromocion canal) {
        return new CanalItem(canal.clave(), canal.nombre());
    }
}
