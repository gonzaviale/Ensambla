package com.ensambla.backend.domain.model.objetivo;

import java.util.List;

/** Cuántas decisiones de cada clase tomó la persona en una semana. */
public record ConteoDecisiones(int aplicadas, int descartadas, int aceptadas, int ignoradas) {

    public static ConteoDecisiones de(List<DecisionObjetivo> decisiones) {
        return new ConteoDecisiones(
                contar(decisiones, TipoDecision.AVISO_APLICADO),
                contar(decisiones, TipoDecision.AVISO_DESCARTADO),
                contar(decisiones, TipoDecision.PASO_ACEPTADO),
                contar(decisiones, TipoDecision.PASO_IGNORADO));
    }

    public int total() {
        return aplicadas + descartadas + aceptadas + ignoradas;
    }

    private static int contar(List<DecisionObjetivo> decisiones, TipoDecision tipo) {
        return (int) decisiones.stream().filter(decision -> decision.tipo() == tipo).count();
    }
}
