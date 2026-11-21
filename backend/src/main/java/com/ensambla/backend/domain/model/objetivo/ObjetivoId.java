package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un objetivo del negocio. */
public record ObjetivoId(UUID valor) implements Identificador {

    public ObjetivoId {
        Objects.requireNonNull(valor, "El id de objetivo es obligatorio");
    }

    public static ObjetivoId de(UUID valor) {
        return new ObjetivoId(valor);
    }

    public static ObjetivoId de(String valor) {
        return new ObjetivoId(UUID.fromString(valor));
    }

    public static ObjetivoId nuevo() {
        return new ObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
