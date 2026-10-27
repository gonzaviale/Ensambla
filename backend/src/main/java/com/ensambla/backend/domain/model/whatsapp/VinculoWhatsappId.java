package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un vínculo de WhatsApp. */
public record VinculoWhatsappId(UUID valor) implements Identificador {

    public VinculoWhatsappId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static VinculoWhatsappId de(UUID valor) {
        return new VinculoWhatsappId(valor);
    }

    public static VinculoWhatsappId de(String valor) {
        return new VinculoWhatsappId(UUID.fromString(valor));
    }

    public static VinculoWhatsappId nuevo() {
        return new VinculoWhatsappId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
