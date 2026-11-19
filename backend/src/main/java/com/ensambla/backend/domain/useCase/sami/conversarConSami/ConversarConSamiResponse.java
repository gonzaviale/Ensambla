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
        return new ConversarConSamiResponse(paso.tipo().name(), preguntaDe(paso), propuestaDe(paso), paso.preguntasHechas());
    }

    public static ConversarConSamiResponse de(InterpretacionSami interpretacion, String origen, int preguntasHechas) {
        return new ConversarConSamiResponse(
                "PROPUESTA", null, PropuestaSamiItem.de(interpretacion, origen), preguntasHechas);
    }

    private static PreguntaSamiItem preguntaDe(PasoConversacionSami paso) {
        return paso.pregunta() == null ? null : PreguntaSamiItem.de(paso.pregunta());
    }

    private static PropuestaSamiItem propuestaDe(PasoConversacionSami paso) {
        if (!paso.tipo().traePropuesta()) {
            return null;
        }
        return PropuestaSamiItem.de(paso.interpretacion(), PropuestaSamiItem.ORIGEN_ALGORITMO);
    }
}