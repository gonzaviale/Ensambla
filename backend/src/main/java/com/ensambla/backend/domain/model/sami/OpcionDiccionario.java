package com.ensambla.backend.domain.model.sami;

/**
 * Respuesta rápida de una pregunta. {@code plantilla} es la que se elige con esta opción;
 * {@code null} significa «ninguna de estas».
 */
public record OpcionDiccionario(String id, String texto, String plantilla) {
}
