package com.ensambla.backend.domain.model.cartel;

import java.util.List;

/** Las fuentes que hoy sugieren qué promocionar: productos caros que no se mueven y combos de baja rotación. */
public final class FuentesDePromocion {

    private FuentesDePromocion() {
    }

    public static List<FuenteDePromocion> todas() {
        return List.of(new PromocionPorProductoEstancado(), new PromocionPorCombo());
    }
}
