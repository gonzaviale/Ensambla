package com.ensambla.backend.infrastructure.presentation.dto.sami;

import com.ensambla.backend.domain.model.plantilla.PlantillaId;

import java.util.List;

/**
 * @param tipo PREGUNTA (viene {@code pregunta}), PROPUESTA (viene {@code propuesta}) o RESPALDO_IA
 *             (el algoritmo no alcanzó y no hay IA disponible)
 */
public record ConversarSamiResponse(
        String tipo,
        Pregunta pregunta,
        Propuesta propuesta,
        int preguntasHechas
) {

    public record Pregunta(String id, String texto, List<Opcion> opciones) {
    }

    public record Opcion(String id, String texto) {
    }

    /** @param origen ALGORITMO (sin costo) o IA */
    public record Propuesta(PlantillaId plantillaId, String plantilla, List<Columna> columnas, double confianza,
                            String origen) {
    }

    public record Columna(String nombre, String tipo, List<String> opciones) {
    }
}
