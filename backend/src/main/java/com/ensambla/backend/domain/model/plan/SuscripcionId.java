package com.ensambla.backend.domain.model.plan;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una suscripción. */
public record SuscripcionId(UUID valor) implements Identificador {

    public SuscripcionId {
        Objects.requireNonNull(valor, "El id de suscripción es obligatorio");
    }

    public static SuscripcionId de(UUID valor) {
        return new SuscripcionId(valor);
    }

    public static SuscripcionId de(String valor) {
        return new SuscripcionId(UUID.fromString(valor));
    }

    public static SuscripcionId nuevo() {
        return new SuscripcionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
