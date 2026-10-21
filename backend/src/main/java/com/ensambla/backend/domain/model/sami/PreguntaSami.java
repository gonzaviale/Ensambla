package com.ensambla.backend.domain.model.sami;

import java.util.List;

/** Pregunta que Sami le hace a la persona, con respuestas rápidas. */
public record PreguntaSami(String id, String texto, List<OpcionSami> opciones) {
}
