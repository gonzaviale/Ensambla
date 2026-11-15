package com.ensambla.backend.domain.model.objetivo;

public enum EstadoMiniObjetivo {
    EN_CURSO,
    LOGRADO,
    /** La persona lo quitó. */
    ABANDONADO,
    /** La persona (o el cierre semanal) lo cambió por otro. */
    REEMPLAZADO
}
