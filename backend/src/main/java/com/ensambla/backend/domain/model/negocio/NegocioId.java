package com.ensambla.backend.domain.model.negocio;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un negocio. */
public record NegocioId(UUID valor) implements Identificador {

    public NegocioId {
        Objects.requireNonNull(valor, "El id de negocio es obligatorio");
    }

    public static NegocioId de(UUID valor) {
        return new NegocioId(valor);
    }

    public static NegocioId de(String valor) {
        return new NegocioId(UUID.fromString(valor));
    }

    public static NegocioId nuevo() {
        return new NegocioId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
