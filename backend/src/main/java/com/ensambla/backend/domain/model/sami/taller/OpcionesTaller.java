package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Una pregunta de Sami con respuestas rápidas. Elegir una opción equivale a escribir su texto; la pantalla siempre
 * suma además la posibilidad de escribir otra cosa.
 */
public record OpcionesTaller(String pregunta, List<String> opciones) {
}
