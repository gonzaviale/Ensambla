package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/** Un audio no se entiende todavía: se le avisa a la persona que mande un texto o una foto. */
@RequiredArgsConstructor
final class ManejadorDeAudio implements ManejadorDeMensaje {

    private final Conversacion conversacion;

    @Override
    public void atender(Atencion atencion, Optional<PropuestaWhatsapp> abierta) {
        conversacion.responder(atencion, TextosWhatsapp.SOLO_TEXTO_Y_FOTOS);
    }
}
