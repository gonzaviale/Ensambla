package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un mensaje de WhatsApp. */
public record MensajeWhatsappId(UUID valor) implements Identificador {

    public MensajeWhatsappId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static MensajeWhatsappId de(UUID valor) {
        return new MensajeWhatsappId(valor);
    }

    public static MensajeWhatsappId de(String valor) {
        return new MensajeWhatsappId(UUID.fromString(valor));
    }

    public static MensajeWhatsappId nuevo() {
        return new MensajeWhatsappId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
