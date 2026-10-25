package com.ensambla.backend.domain.model.registro;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un registro (fila) de un bloque. */
public record RegistroId(UUID valor) implements Identificador {

    public RegistroId {
        Objects.requireNonNull(valor, "El id de registro es obligatorio");
    }

    public static RegistroId de(UUID valor) {
        return new RegistroId(valor);
    }

    public static RegistroId de(String valor) {
        return new RegistroId(UUID.fromString(valor));
    }

    public static RegistroId nuevo() {
        return new RegistroId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
