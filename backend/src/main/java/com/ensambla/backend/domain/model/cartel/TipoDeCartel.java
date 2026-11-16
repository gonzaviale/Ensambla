package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;

/** Las plantillas de contenido: qué se anuncia y cuántas líneas de detalle admite. */
public enum TipoDeCartel {

    PROMO_PRECIO(1),
    DOS_POR_UNO(1),
    COMBO(4),
    NOVEDAD(1),
    HORARIOS(7),
    LISTA_PRECIOS(10);

    private final int maximoDeLineas;

    TipoDeCartel(int maximoDeLineas) {
        this.maximoDeLineas = maximoDeLineas;
    }

    public void exigirLineasAdmitidas(int cantidad) {
        if (cantidad > maximoDeLineas) {
            throw new BusinessRuleException("Un cartel de este tipo admite hasta " + maximoDeLineas + " líneas");
        }
    }
}
