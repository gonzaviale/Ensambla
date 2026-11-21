package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

/** La persona dijo que no: no se guarda nada. */
@RequiredArgsConstructor
final class DescartarPropuesta implements ReaccionAUnaPropuesta {

    private final PropuestasDelChat propuestas;

    @Override
    public void ante(PropuestaWhatsapp propuesta, Atencion atencion) {
        propuestas.descartarAvisando(propuesta, atencion, TextosWhatsapp.DESCARTADO);
    }
}
