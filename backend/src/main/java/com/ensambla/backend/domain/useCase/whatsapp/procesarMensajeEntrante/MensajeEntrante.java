package com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante;

import com.ensambla.backend.domain.model.whatsapp.TipoMensajeWhatsapp;

import java.time.OffsetDateTime;

/**
 * Mensaje de WhatsApp ya leído del webhook, sin depender del proveedor que lo entregó.
 *
 * @param telefono    remitente, sólo dígitos con código de país
 * @param idExterno   id del mensaje en WhatsApp
 * @param texto       contenido si es texto; null en una nota de voz
 * @param mimeAudio   tipo del audio si es una nota de voz; null en un texto
 * @param duracionSeg duración de la nota de voz; null si no se informó o es texto
 */
public record MensajeEntrante(
        String telefono,
        String idExterno,
        TipoMensajeWhatsapp tipo,
        String texto,
        String mimeAudio,
        Integer duracionSeg,
        OffsetDateTime recibidoEn
) {
}
