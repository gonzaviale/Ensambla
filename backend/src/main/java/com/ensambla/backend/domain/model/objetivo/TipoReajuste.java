package com.ensambla.backend.domain.model.objetivo;

/** Qué cambió el cierre semanal en un mini-objetivo. */
public enum TipoReajuste {
    /** Se logró: sigue el mismo mini-objetivo con una meta más alta. */
    SUBIR_META,
    /** No avanzaba, se quitó o ya no se puede medir: se cambia por el mejor candidato. */
    REEMPLAZAR,
    /** Una decisión no movió la métrica: se prueba con un candidato de otro tipo. */
    PROBAR_OTRO_CAMINO
}
