package com.ensambla.backend.domain.model.sami.taller;

import java.util.Map;

/**
 * Una acción ya validada que el taller tiene que aplicar. Los argumentos usan el vocabulario del catálogo;
 * {@code pieza}, {@code desde} y {@code hacia} son el id de una pieza existente o, si se sumó en este mismo turno, su
 * tipo (una tabla a medida, que se repite, por su nombre).
 */
public record AccionTaller(String herramienta, Map<String, Object> argumentos) {
}
