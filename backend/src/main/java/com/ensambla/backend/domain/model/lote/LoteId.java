package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un lote de fotos del cuaderno. */
public record LoteId(UUID valor) implements Identificador {

    public LoteId {
        Objects.requireNonNull(valor, "El id de lote es obligatorio");
    }

    public static LoteId de(UUID valor) {
        return new LoteId(valor);
    }

    public static LoteId de(String valor) {
        return new LoteId(UUID.fromString(valor));
    }

    public static LoteId nuevo() {
        return new LoteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
