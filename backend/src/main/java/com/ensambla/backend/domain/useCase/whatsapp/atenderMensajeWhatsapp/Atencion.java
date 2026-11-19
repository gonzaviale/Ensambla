package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;

import java.time.OffsetDateTime;

/** Un mensaje que se está atendiendo: del negocio que lo mandó y a la hora en que se lo atiende. */
record Atencion(MensajeWhatsapp mensaje, NegocioId negocioId, OffsetDateTime ahora) {

    String texto() {
        return mensaje.getTexto();
    }

    String telefono() {
        return mensaje.getTelefono();
    }
}
