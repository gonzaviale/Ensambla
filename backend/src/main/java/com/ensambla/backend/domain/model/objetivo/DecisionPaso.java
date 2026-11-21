package com.ensambla.backend.domain.model.objetivo;

/** Lo que la persona puede responder al paso de un mini-objetivo. */
public enum DecisionPaso {
    ACEPTADO(TipoDecision.PASO_ACEPTADO),
    IGNORADO(TipoDecision.PASO_IGNORADO);

    private final TipoDecision tipo;

    DecisionPaso(TipoDecision tipo) {
        this.tipo = tipo;
    }

    public TipoDecision tipo() {
        return tipo;
    }
}
