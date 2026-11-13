package com.ensambla.backend.domain.model.objetivo;

/** Qué decidió la persona frente a un objetivo. */
public enum TipoDecision {
    /** Aplicó la sugerencia de un aviso. */
    AVISO_APLICADO,
    /** Descartó un aviso. */
    AVISO_DESCARTADO,
    /** Aceptó el paso de un mini-objetivo («Lo hago»). */
    PASO_ACEPTADO,
    /** Dijo que ahora no al paso de un mini-objetivo; es explícito, no se deduce de no hacer nada. */
    PASO_IGNORADO
}
