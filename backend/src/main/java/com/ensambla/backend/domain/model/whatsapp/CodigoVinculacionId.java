package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un código de vinculación. */
public record CodigoVinculacionId(UUID valor) implements Identificador {

    public CodigoVinculacionId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static CodigoVinculacionId de(UUID valor) {
        return new CodigoVinculacionId(valor);
    }

    public static CodigoVinculacionId de(String valor) {
        return new CodigoVinculacionId(UUID.fromString(valor));
    }

    public static CodigoVinculacionId nuevo() {
        return new CodigoVinculacionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
