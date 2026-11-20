package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Un dato que un componente del editor trae de fábrica.
 *
 * @param clave    nombre interno del frontend
 * @param etiqueta cómo lo ve la persona
 * @param opciones sólo para los datos de tipo {@code select}; vacía en el resto
 * @param fija     si Ensambla necesita el dato para funcionar: se puede renombrar, pero no quitar ni cambiar de tipo
 */
public record CampoComponente(String clave, String etiqueta, String tipo, boolean obligatorio, List<String> opciones,
                              boolean fija) {

    public CampoComponente {
        opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
    }

    /** Un dato que no es fijo. */
    public CampoComponente(String clave, String etiqueta, String tipo, boolean obligatorio, List<String> opciones) {
        this(clave, etiqueta, tipo, obligatorio, opciones, false);
    }
}
