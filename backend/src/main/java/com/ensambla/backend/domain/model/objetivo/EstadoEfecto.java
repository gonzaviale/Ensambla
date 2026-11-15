package com.ensambla.backend.domain.model.objetivo;

/** Qué se sabe del efecto de una decisión sobre las ventas. */
public enum EstadoEfecto {
    /** Ya se midió: se compara lo que se vendía antes con lo que se vendió después. */
    MEDIDO,
    /** Se va a medir cuando termine la ventana de días posteriores a la decisión. */
    MIDIENDO,
    /** Este tipo de decisión no se mide. */
    SIN_MEDICION
}
