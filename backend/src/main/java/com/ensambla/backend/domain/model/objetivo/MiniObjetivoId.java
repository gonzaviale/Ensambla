package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un mini-objetivo. */
public record MiniObjetivoId(UUID valor) implements Identificador {

    public MiniObjetivoId {
        Objects.requireNonNull(valor, "El id de mini-objetivo es obligatorio");
    }

    public static MiniObjetivoId de(UUID valor) {
        return new MiniObjetivoId(valor);
    }

    public static MiniObjetivoId de(String valor) {
        return new MiniObjetivoId(UUID.fromString(valor));
    }

    public static MiniObjetivoId nuevo() {
        return new MiniObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
