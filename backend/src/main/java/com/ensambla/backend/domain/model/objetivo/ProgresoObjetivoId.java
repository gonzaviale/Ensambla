package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una foto del avance de un objetivo. */
public record ProgresoObjetivoId(UUID valor) implements Identificador {

    public ProgresoObjetivoId {
        Objects.requireNonNull(valor, "El id de progreso de objetivo es obligatorio");
    }

    public static ProgresoObjetivoId de(UUID valor) {
        return new ProgresoObjetivoId(valor);
    }

    public static ProgresoObjetivoId de(String valor) {
        return new ProgresoObjetivoId(UUID.fromString(valor));
    }

    public static ProgresoObjetivoId nuevo() {
        return new ProgresoObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
