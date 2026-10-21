package com.ensambla.backend.domain.model.sami;

import java.util.List;
import java.util.Objects;

/** Términos que identifican a una plantilla; {@code plantilla} es su nombre tal como figura en la base. */
public record PlantillaDiccionario(String plantilla, List<TerminoDiccionario> terminos) {

    public PlantillaDiccionario {
        terminos = List.copyOf(Objects.requireNonNullElse(terminos, List.of()));
    }
}
