package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una zona. */
public record ZonaId(UUID valor) implements Identificador {

    public ZonaId {
        Objects.requireNonNull(valor, "El id de zona es obligatorio");
    }

    public static ZonaId de(UUID valor) {
        return new ZonaId(valor);
    }

    public static ZonaId de(String valor) {
        return new ZonaId(UUID.fromString(valor));
    }

    public static ZonaId nuevo() {
        return new ZonaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
