package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Un dato que un componente del editor trae de fábrica.
 *
 * @param clave    nombre interno del frontend
 * @param etiqueta cómo lo ve la persona
 * @param opciones sólo para los datos de tipo {@code select}; vacía en el resto
 */
public record CampoComponente(String clave, String etiqueta, String tipo, boolean obligatorio, List<String> opciones) {

    public CampoComponente {
        opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
    }
}
