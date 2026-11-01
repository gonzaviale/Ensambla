package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.model.sami.PreguntaSami;

import java.util.List;

public record PreguntaSamiItem(String id, String texto, List<OpcionSamiItem> opciones) {

    static PreguntaSamiItem de(PreguntaSami pregunta) {
        return new PreguntaSamiItem(
                pregunta.id(),
                pregunta.texto(),
                pregunta.opciones().stream().map(opcion -> new OpcionSamiItem(opcion.id(), opcion.texto())).toList());
    }
}
