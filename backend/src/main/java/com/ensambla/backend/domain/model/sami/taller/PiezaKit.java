package com.ensambla.backend.domain.model.sami.taller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Una pieza de un kit por rubro: qué componente usa, cómo se llama, qué datos suma y con qué filas de ejemplo arranca.
 *
 * @param campos   datos además de los que trae el componente
 * @param ejemplos filas {@code {etiqueta: valor}} con etiquetas del componente o de {@code campos}
 */
public record PiezaKit(String tipo, String nombre, List<CampoNuevo> campos, List<Map<String, Object>> ejemplos) {

    public PiezaKit {
        campos = List.copyOf(Objects.requireNonNullElse(campos, List.of()));
        ejemplos = List.copyOf(Objects.requireNonNullElse(ejemplos, List.of()));
    }

    /** Los argumentos de {@code agregar_pieza} que suman esta pieza. */
    public Map<String, Object> comoArgumentos() {
        Map<String, Object> argumentos = new LinkedHashMap<>();
        argumentos.put("tipo", tipo);
        argumentos.put("nombre", nombre);
        argumentos.put("campos", campos.stream().map(CampoNuevo::comoArgumentos).toList());
        argumentos.put("ejemplos", ejemplos);
        return argumentos;
    }
}
