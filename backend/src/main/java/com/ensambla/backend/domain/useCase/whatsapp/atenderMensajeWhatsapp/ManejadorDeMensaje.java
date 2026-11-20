package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;

import java.util.Optional;

/** Cómo atiende Sami cada tipo de mensaje que llega por WhatsApp: un texto, una foto o un audio. */
interface ManejadorDeMensaje {

    /** @param abierta la carga que la persona todavía no confirmó y que sigue vigente, si hay una */
    void atender(Atencion atencion, Optional<PropuestaWhatsapp> abierta);

    /** Si el mensaje contesta a una carga abierta (un sí, un no o el número de una tabla). */
    default boolean esUnaRespuesta(Atencion atencion) {
        return false;
    }
}
