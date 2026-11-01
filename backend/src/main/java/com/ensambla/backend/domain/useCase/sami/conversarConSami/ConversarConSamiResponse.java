package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PasoConversacionSami;

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

    public static ConversarConSamiResponse de(PasoConversacionSami paso) {
        PreguntaSamiItem pregunta = paso.pregunta() == null ? null : PreguntaSamiItem.de(paso.pregunta());
        PropuestaSamiItem propuesta = switch (paso.tipo()) {
            case PROPUESTA -> PropuestaSamiItem.de(paso.interpretacion(), PropuestaSamiItem.ORIGEN_ALGORITMO);
            case PREGUNTA, RESPALDO_IA -> null;
        };
        return new ConversarConSamiResponse(paso.tipo().name(), pregunta, propuesta, paso.preguntasHechas());
    }

    public static ConversarConSamiResponse de(InterpretacionSami interpretacion, String origen, int preguntasHechas) {
        return new ConversarConSamiResponse(
                "PROPUESTA", null, PropuestaSamiItem.de(interpretacion, origen), preguntasHechas);
    }
}
