package com.ensambla.backend.domain.model.proyecto;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un proyecto (sistema). */
public record ProyectoId(UUID valor) implements Identificador {

    public ProyectoId {
        Objects.requireNonNull(valor, "El id de proyecto es obligatorio");
    }

    public static ProyectoId de(UUID valor) {
        return new ProyectoId(valor);
    }

    public static ProyectoId de(String valor) {
        return new ProyectoId(UUID.fromString(valor));
    }

    public static ProyectoId nuevo() {
        return new ProyectoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
