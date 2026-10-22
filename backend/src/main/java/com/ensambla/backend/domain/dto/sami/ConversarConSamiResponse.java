package com.ensambla.backend.domain.dto.sami;

/**
 * Qué sigue en la conversación: {@code tipo} es PREGUNTA (viene {@code pregunta}), PROPUESTA (viene
 * {@code propuesta}) o RESPALDO_IA (el algoritmo no alcanzó y no hay IA disponible).
 */
public record ConversarConSamiResponse(
        String tipo,
        PreguntaSamiItem pregunta,
        PropuestaSamiItem propuesta,
        int preguntasHechas
) {
}
