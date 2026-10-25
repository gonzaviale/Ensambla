package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una foto de un lote. */
public record FotoId(UUID valor) implements Identificador {

    public FotoId {
        Objects.requireNonNull(valor, "El id de foto es obligatorio");
    }

    public static FotoId de(UUID valor) {
        return new FotoId(valor);
    }

    public static FotoId de(String valor) {
        return new FotoId(UUID.fromString(valor));
    }

    public static FotoId nuevo() {
        return new FotoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
