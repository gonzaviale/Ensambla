package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un reclamo de un lote por el worker de lectura. */
public record ReclamoId(UUID valor) implements Identificador {

    public ReclamoId {
        Objects.requireNonNull(valor, "El id del reclamo es obligatorio");
    }

    public static ReclamoId de(UUID valor) {
        return new ReclamoId(valor);
    }

    public static ReclamoId de(String valor) {
        return new ReclamoId(UUID.fromString(valor));
    }

    public static ReclamoId nuevo() {
        return new ReclamoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
