package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

/** La persona escribió otra cosa en vez de contestar: la carga anterior se descarta y el texto nuevo se interpreta. */
@RequiredArgsConstructor
final class ReemplazarPropuesta implements ReaccionAUnaPropuesta {

    private final PropuestasDelChat propuestas;
    private final InterpretarTexto interpretarTexto;

    @Override
    public void ante(PropuestaWhatsapp propuesta, Atencion atencion) {
        propuestas.descartarAvisando(propuesta, atencion, TextosWhatsapp.DESCARTE_ANTERIOR);
        interpretarTexto.interpretar(atencion);
    }
}
