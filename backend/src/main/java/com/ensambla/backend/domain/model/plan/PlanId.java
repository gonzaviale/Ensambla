package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un plan. */
public record PlanId(UUID valor) implements Identificador {

    public PlanId {
        Objects.requireNonNull(valor, "El id de plan es obligatorio");
    }

    public static PlanId de(UUID valor) {
        return new PlanId(valor);
    }

    public static PlanId de(String valor) {
        return new PlanId(UUID.fromString(valor));
    }

    public static PlanId nuevo() {
        return new PlanId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
