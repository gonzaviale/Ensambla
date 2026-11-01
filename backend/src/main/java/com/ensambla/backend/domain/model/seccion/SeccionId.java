package com.ensambla.backend.domain.model.seccion;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una sección. */
public record SeccionId(UUID valor) implements Identificador {

    public SeccionId {
        Objects.requireNonNull(valor, "El id de sección es obligatorio");
    }

    public static SeccionId de(UUID valor) {
        return new SeccionId(valor);
    }

    public static SeccionId de(String valor) {
        return new SeccionId(UUID.fromString(valor));
    }

    public static SeccionId nuevo() {
        return new SeccionId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
