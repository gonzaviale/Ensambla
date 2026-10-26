package com.ensambla.backend.domain.model.campo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un campo de un bloque. */
public record CampoId(UUID valor) implements Identificador {

    public CampoId {
        Objects.requireNonNull(valor, "El id de campo es obligatorio");
    }

    public static CampoId de(UUID valor) {
        return new CampoId(valor);
    }

    public static CampoId de(String valor) {
        return new CampoId(UUID.fromString(valor));
    }

    public static CampoId nuevo() {
        return new CampoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
