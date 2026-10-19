package com.ensambla.backend.infrastructure.presentation.dto.sami;

import java.util.List;
import java.util.Map;

/**
 * @param acciones lo que el taller tiene que aplicar; {@code pieza} es el id de una pieza existente o el tipo de una
 *                 que se sumó en este mismo turno
 * @param opciones una pregunta con respuestas rápidas, o {@code null}; la pantalla siempre suma «escribir otra cosa»
 */
public record ConversarTallerResponse(String respuesta, List<Accion> acciones, Opciones opciones) {

    public record Accion(String herramienta, Map<String, Object> argumentos) {
    }

    public record Opciones(String pregunta, List<String> opciones) {
    }
}
