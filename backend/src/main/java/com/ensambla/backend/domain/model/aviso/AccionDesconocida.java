package com.ensambla.backend.domain.model.aviso;

import java.util.Map;

/** Una acción de un tipo que esta versión no conoce: se conserva tal cual y no hace nada al aplicarla. */
public record AccionDesconocida(String tipo, Map<String, Object> datos) implements AccionDeAviso {
}
