package com.ensambla.backend.domain.dto.sami;

import java.util.List;
import java.util.UUID;

/**
 * Estructura que Sami propone: la plantilla base y las columnas que le suma.
 *
 * @param origen ALGORITMO (diccionario y preguntas, sin costo) o IA
 */
public record PropuestaSamiItem(
        UUID plantillaId,
        String plantilla,
        List<ColumnaSamiItem> columnas,
        double confianza,
        String origen
) {
}
