package com.ensambla.backend.domain.dto.whatsapp;

import java.time.OffsetDateTime;
import java.util.List;

/** Los mensajes de un aviso de Meta; un aviso puede traer varios. */
public record RecibirMensajesWhatsappRequest(List<MensajeEntrante> mensajes) {

    /**
     * @param tipo    {@code TEXTO} o {@code AUDIO}
     * @param texto   el texto, si es {@code TEXTO}
     * @param mediaId id del audio en Meta, si es {@code AUDIO}
     */
    public record MensajeEntrante(
            String waMensajeId,
            String telefono,
            String tipo,
            String texto,
            String mediaId,
            OffsetDateTime recibidoEn
    ) {
    }
}
