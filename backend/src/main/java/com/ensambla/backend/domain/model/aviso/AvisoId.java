package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un aviso. */
public record AvisoId(UUID valor) implements Identificador {

    public AvisoId {
        Objects.requireNonNull(valor, "El id de aviso es obligatorio");
    }

    public static AvisoId de(UUID valor) {
        return new AvisoId(valor);
    }

    public static AvisoId de(String valor) {
        return new AvisoId(UUID.fromString(valor));
    }

    public static AvisoId nuevo() {
        return new AvisoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
