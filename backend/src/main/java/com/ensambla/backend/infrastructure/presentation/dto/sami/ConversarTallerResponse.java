package com.ensambla.backend.infrastructure.presentation.dto.sami;

import java.util.List;
import java.util.Map;

/**
 * @param acciones lo que el taller tiene que aplicar; {@code pieza} es el id de una pieza existente o el tipo de una
 *                 que se sumó en este mismo turno
 * @param preguntas las dudas del turno con respuestas rápidas (vacía si no hay): la pantalla las muestra de a una y
 *                  siempre suma «escribir otra cosa» y «saltear»
 */
public record ConversarTallerResponse(String respuesta, List<Accion> acciones, List<Opciones> preguntas) {

    public record Accion(String herramienta, Map<String, Object> argumentos) {
    }

    public record Opciones(String tema, String pregunta, List<String> opciones) {
    }
}
