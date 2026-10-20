package com.ensambla.backend.domain.model.sami.taller;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Una herramienta que el modelo pidió invocar, todavía sin validar. Los argumentos pueden traer nulos. */
public record LlamadaHerramienta(String nombre, Map<String, Object> argumentos) {

    public LlamadaHerramienta {
        argumentos = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNullElse(argumentos, Map.of())));
    }
}
