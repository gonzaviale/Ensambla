package com.ensambla.backend.domain.model.objetivo;

/** Hacia dónde tiene que ir la métrica de un mini-objetivo para cumplirlo. */
public enum SentidoMiniObjetivo {
    /** Se cumple cuando el valor llega a la meta o la pasa. */
    SUBE,
    /** Se cumple cuando el valor baja hasta la meta o por debajo. */
    BAJA
}
