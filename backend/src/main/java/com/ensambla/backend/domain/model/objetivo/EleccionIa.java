package com.ensambla.backend.domain.model.objetivo;

/**
 * Lo que la IA eligió para un mini-objetivo: una métrica del catálogo por su {@code codigo} (medido) o el texto de una
 * acción (manual). El resto de los campos viene vacío.
 */
public record EleccionIa(ModoMiniObjetivo modo, String codigo, String titulo, String justificacion) {
}
