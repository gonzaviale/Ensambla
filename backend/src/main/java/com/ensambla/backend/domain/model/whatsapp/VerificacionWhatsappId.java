package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una verificación del número. */
public record VerificacionWhatsappId(UUID valor) implements Identificador {

    public VerificacionWhatsappId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static VerificacionWhatsappId de(UUID valor) {
        return new VerificacionWhatsappId(valor);
    }

    public static VerificacionWhatsappId de(String valor) {
        return new VerificacionWhatsappId(UUID.fromString(valor));
    }

    public static VerificacionWhatsappId nuevo() {
        return new VerificacionWhatsappId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
