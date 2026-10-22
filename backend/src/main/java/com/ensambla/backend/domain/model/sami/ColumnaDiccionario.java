package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.enums.TipoCampo;

import java.util.List;
import java.util.Objects;

/**
 * Columna opcional que Sami puede sumar a una plantilla.
 *
 * @param disparadores palabras o frases que la activan
 * @param plantillas   plantillas donde tiene sentido; vacío significa cualquiera
 */
public record ColumnaDiccionario(
        String nombre,
        TipoCampo tipo,
        List<String> opciones,
        List<String> disparadores,
        List<String> plantillas
) {

    public ColumnaDiccionario {
        opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
        disparadores = List.copyOf(Objects.requireNonNullElse(disparadores, List.of()));
        plantillas = List.copyOf(Objects.requireNonNullElse(plantillas, List.of()));
    }
}
