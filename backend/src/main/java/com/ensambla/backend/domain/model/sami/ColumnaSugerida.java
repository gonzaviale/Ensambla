package com.ensambla.backend.domain.model.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;

import java.util.List;
import java.util.Objects;

/** Columna extra que Sami propone sumar a la plantilla sugerida. */
public record ColumnaSugerida(String nombre, TipoCampo tipo, List<String> opciones) {

    public ColumnaSugerida {
        opciones = Objects.requireNonNullElse(opciones, List.of());
    }
}
