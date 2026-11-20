package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.model.whatsapp.PropuestaWhatsapp;

/** Qué hace Sami cuando la persona contesta algo teniendo una carga abierta: confirmarla, descartarla, elegir tabla o empezar de nuevo. */
interface ReaccionAUnaPropuesta {

    void ante(PropuestaWhatsapp propuesta, Atencion atencion);
}
