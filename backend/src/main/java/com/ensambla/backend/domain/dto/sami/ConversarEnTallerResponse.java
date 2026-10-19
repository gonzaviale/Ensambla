package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.Map;

/**
 * Un turno de Sami en el taller.
 *
 * @param acciones lo que el taller tiene que aplicar, ya validado
 * @param opciones una pregunta con respuestas rápidas; {@code null} si no hay
 */
public record ConversarEnTallerResponse(
        String respuesta,
        List<AccionTallerItem> acciones,
        OpcionesTallerItem opciones
) {

    public record AccionTallerItem(String herramienta, Map<String, Object> argumentos) {
    }

    public record OpcionesTallerItem(String pregunta, List<String> opciones) {
    }
}
