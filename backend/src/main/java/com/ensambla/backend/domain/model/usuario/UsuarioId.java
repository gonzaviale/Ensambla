package com.ensambla.backend.domain.model.usuario;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un usuario. */
public record UsuarioId(UUID valor) implements Identificador {

    public UsuarioId {
        Objects.requireNonNull(valor, "El id de usuario es obligatorio");
    }

    public static UsuarioId de(UUID valor) {
        return new UsuarioId(valor);
    }

    public static UsuarioId de(String valor) {
        return new UsuarioId(UUID.fromString(valor));
    }

    public static UsuarioId nuevo() {
        return new UsuarioId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
