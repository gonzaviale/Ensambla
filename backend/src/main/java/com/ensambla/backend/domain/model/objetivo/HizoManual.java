package com.ensambla.backend.domain.model.objetivo;

/** Si la persona hizo la acción de un mini-objetivo manual esa semana. */
public enum HizoManual {
    SI(TipoDecision.MINI_HECHO),
    EN_PARTE(TipoDecision.MINI_EN_PARTE),
    NO(TipoDecision.MINI_NO_HECHO);

    private final TipoDecision tipoDecision;

    HizoManual(TipoDecision tipoDecision) {
        this.tipoDecision = tipoDecision;
    }

    /** La decisión que queda anotada en el historial del objetivo. */
    public TipoDecision tipoDecision() {
        return tipoDecision;
    }
}
