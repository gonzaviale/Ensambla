package com.ensambla.backend.domain.model.pago;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una renovación de Premium. */
public record RenovacionId(UUID valor) implements Identificador {

    public RenovacionId {
        Objects.requireNonNull(valor, "El id de renovación es obligatorio");
    }

    public static RenovacionId de(UUID valor) {
        return new RenovacionId(valor);
    }

    public static RenovacionId de(String valor) {
        return new RenovacionId(UUID.fromString(valor));
    }

    public static RenovacionId nuevo() {
        return new RenovacionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
