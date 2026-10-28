package com.ensambla.backend.domain.model.sami.taller;

import java.util.Map;

/**
 * Un registro real que ya está cargado en una pieza del taller (por ejemplo, uno que Sami leyó de una foto en este
 * mismo armado). Viaja al modelo para que pueda corregirlo con {@code editar_registros} en vez de cargarlo de nuevo.
 *
 * @param id    el id de la fila en el taller
 * @param datos los valores por etiqueta del dato
 */
public record RegistroTaller(String id, Map<String, Object> datos) {
}
