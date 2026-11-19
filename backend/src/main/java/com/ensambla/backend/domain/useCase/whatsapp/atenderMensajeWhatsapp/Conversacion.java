package com.ensambla.backend.domain.useCase.whatsapp.atenderMensajeWhatsapp;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.whatsapp.ArchivoWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.MensajeWhatsapp;
import com.ensambla.backend.domain.model.whatsapp.TextosWhatsapp;
import com.ensambla.backend.domain.port.MensajeriaWhatsappPort;
import lombok.RequiredArgsConstructor;

/** Lo que Sami le dice a la persona por el chat y lo que le baja del chat (la foto que mandó). */
@RequiredArgsConstructor
final class Conversacion {

    private final MensajeriaWhatsappPort mensajeria;

    void responder(Atencion atencion, String texto) {
        responder(atencion.mensaje(), texto);
    }

    void responder(MensajeWhatsapp mensaje, String texto) {
        mensajeria.enviarTexto(mensaje.getTelefono(), texto);
    }

    /** Se baja antes de reservar la página: si no se puede bajar, no se cobra nada. */
    ArchivoWhatsapp descargarFoto(MensajeWhatsapp mensaje) {
        try {
            return mensajeria.descargarMedia(mensaje.getIdExterno());
        } catch (RuntimeException e) {
            throw new BusinessRuleException(TextosWhatsapp.FOTO_NO_DESCARGADA);
        }
    }
}
