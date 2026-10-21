package com.ensambla.backend.domain.model.sami.taller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Un dato que se suma a una pieza además de los que trae su componente.
 *
 * @param opciones sólo para los datos de tipo {@code select}; vacía en el resto
 */
public record CampoNuevo(String etiqueta, String tipo, List<String> opciones) {

    public CampoNuevo {
        opciones = List.copyOf(Objects.requireNonNullElse(opciones, List.of()));
    }

    public CampoTaller comoCampo() {
        return new CampoTaller(etiqueta, tipo);
    }

    /** Como argumento de una acción: {@code opciones} sólo aparece si tiene alguna. */
    public Map<String, Object> comoArgumentos() {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("etiqueta", etiqueta);
        argumentos.put("tipo", tipo);
        if (!opciones.isEmpty()) {
            argumentos.put("opciones", opciones);
        }
        return argumentos;
    }
}
