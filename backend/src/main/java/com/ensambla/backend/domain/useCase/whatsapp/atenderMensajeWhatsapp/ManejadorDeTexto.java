package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.RespuestaChat;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Optional;

/**
 * Un texto contesta a la carga abierta (sí, no o el número de una tabla) o es un pedido nuevo que Sami interpreta.
 */
@RequiredArgsConstructor
final class ManejadorDeTexto implements ManejadorDeMensaje {

    private final Map<RespuestaChat, ReaccionAUnaPropuesta> reacciones;
    private final InterpretarTexto interpretarTexto;

    @Override
    public void atender(Atencion atencion, Optional<PropuestaWhatsapp> abierta) {
        abierta.ifPresentOrElse(
                propuesta -> reaccionA(atencion).ante(propuesta, atencion),
                () -> interpretarTexto.interpretar(atencion));
    }

    @Override
    public boolean esUnaRespuesta(Atencion atencion) {
        return RespuestaChat.de(atencion.texto()) != RespuestaChat.OTRO;
    }

    private ReaccionAUnaPropuesta reaccionA(Atencion atencion) {
        return reacciones.get(RespuestaChat.de(atencion.texto()));
    }
}
