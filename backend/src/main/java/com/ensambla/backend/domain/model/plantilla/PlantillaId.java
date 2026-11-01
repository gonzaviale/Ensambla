package com.ensambla.backend.domain.model.plantilla;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una plantilla. */
public record PlantillaId(UUID valor) implements Identificador {

    public PlantillaId {
        Objects.requireNonNull(valor, "El id de plantilla es obligatorio");
    }

    public static PlantillaId de(UUID valor) {
        return new PlantillaId(valor);
    }

    public static PlantillaId de(String valor) {
        return new PlantillaId(UUID.fromString(valor));
    }

    public static PlantillaId nuevo() {
        return new PlantillaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
