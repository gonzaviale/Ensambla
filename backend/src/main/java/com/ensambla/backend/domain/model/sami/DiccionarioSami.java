package com.ensambla.backend.domain.model.sami;

import java.util.List;
import java.util.Objects;

/**
 * Vocabulario con el que Sami deduce la plantilla y las columnas a partir de un texto libre (T-115) y
 * preguntas para desempatar cuando el texto no alcanza (T-116).
 */
public record DiccionarioSami(
        List<PlantillaDiccionario> plantillas,
        List<ColumnaDiccionario> columnas,
        List<PreguntaDiccionario> preguntas
) {

    public DiccionarioSami {
        plantillas = List.copyOf(Objects.requireNonNullElse(plantillas, List.of()));
        columnas = List.copyOf(Objects.requireNonNullElse(columnas, List.of()));
        preguntas = List.copyOf(Objects.requireNonNullElse(preguntas, List.of()));
    }
}
