package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una decisión de la persona frente a un objetivo. */
public record DecisionObjetivoId(UUID valor) implements Identificador {

    public DecisionObjetivoId {
        Objects.requireNonNull(valor, "El id de decisión de objetivo es obligatorio");
    }

    public static DecisionObjetivoId de(UUID valor) {
        return new DecisionObjetivoId(valor);
    }

    public static DecisionObjetivoId de(String valor) {
        return new DecisionObjetivoId(UUID.fromString(valor));
    }

    public static DecisionObjetivoId nuevo() {
        return new DecisionObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
