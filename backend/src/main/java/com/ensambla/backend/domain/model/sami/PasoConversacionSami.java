package com.ensambla.backend.domain.model.sami;

/**
 * Qué sigue en la conversación con Sami. {@code pregunta} sólo viene en {@link TipoPaso#PREGUNTA};
 * {@code interpretacion} siempre trae lo deducido hasta ahora.
 */
public record PasoConversacionSami(
        TipoPaso tipo,
        PreguntaSami pregunta,
        InterpretacionSami interpretacion,
        int preguntasHechas
) {
}
