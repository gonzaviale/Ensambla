package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;
import java.util.Objects;

/**
 * Resumen del sistema que se arma en el taller: nombre, distribución y piezas con sus campos y sus registros reales.
 * No lleva filas de ejemplo, para que cada turno con la IA cueste pocos tokens.
 */
public record EstadoTaller(String nombre, String distribucion, List<PiezaTaller> piezas) {

    public EstadoTaller {
        piezas = List.copyOf(Objects.requireNonNullElse(piezas, List.of()));
    }
}
