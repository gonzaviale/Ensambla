package com.ensambla.backend.domain.model.sami;

import java.util.List;
import java.util.Objects;

/** Pregunta que separa a un grupo de plantillas: las que aparecen en sus opciones. */
public record PreguntaDiccionario(String id, String texto, List<OpcionDiccionario> opciones) {

    public PreguntaDiccionario {
        opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
    }
}
