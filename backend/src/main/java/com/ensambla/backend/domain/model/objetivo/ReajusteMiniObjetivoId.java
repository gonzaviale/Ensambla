package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un cambio que el cierre semanal hizo en un mini-objetivo. */
public record ReajusteMiniObjetivoId(UUID valor) implements Identificador {

    public ReajusteMiniObjetivoId {
        Objects.requireNonNull(valor, "El id de reajuste de mini-objetivo es obligatorio");
    }

    public static ReajusteMiniObjetivoId de(UUID valor) {
        return new ReajusteMiniObjetivoId(valor);
    }

    public static ReajusteMiniObjetivoId de(String valor) {
        return new ReajusteMiniObjetivoId(UUID.fromString(valor));
    }

    public static ReajusteMiniObjetivoId nuevo() {
        return new ReajusteMiniObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
