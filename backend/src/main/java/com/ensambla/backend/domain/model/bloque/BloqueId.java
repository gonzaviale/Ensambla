package com.ensambla.backend.domain.model.bloque;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un bloque (tabla). */
public record BloqueId(UUID valor) implements Identificador {

    public BloqueId {
        Objects.requireNonNull(valor, "El id de bloque es obligatorio");
    }

    public static BloqueId de(UUID valor) {
        return new BloqueId(valor);
    }

    public static BloqueId de(String valor) {
        return new BloqueId(UUID.fromString(valor));
    }

    public static BloqueId nuevo() {
        return new BloqueId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
