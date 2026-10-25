package com.ensambla.backend.domain.model.receta;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una receta. */
public record RecetaId(UUID valor) implements Identificador {

    public RecetaId {
        Objects.requireNonNull(valor, "El id de receta es obligatorio");
    }

    public static RecetaId de(UUID valor) {
        return new RecetaId(valor);
    }

    public static RecetaId de(String valor) {
        return new RecetaId(UUID.fromString(valor));
    }

    public static RecetaId nuevo() {
        return new RecetaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
