package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador del kit de marca de un negocio. */
public record KitDeMarcaId(UUID valor) implements Identificador {

    public KitDeMarcaId {
        Objects.requireNonNull(valor, "El id del kit de marca es obligatorio");
    }

    public static KitDeMarcaId de(UUID valor) {
        return new KitDeMarcaId(valor);
    }

    public static KitDeMarcaId de(String valor) {
        return new KitDeMarcaId(UUID.fromString(valor));
    }

    public static KitDeMarcaId nuevo() {
        return new KitDeMarcaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
