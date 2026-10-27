package com.ensambla.backend.domain.model.whatsapp;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una carga propuesta por Sami. */
public record PropuestaWhatsappId(UUID valor) implements Identificador {

    public PropuestaWhatsappId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static PropuestaWhatsappId de(UUID valor) {
        return new PropuestaWhatsappId(valor);
    }

    public static PropuestaWhatsappId de(String valor) {
        return new PropuestaWhatsappId(UUID.fromString(valor));
    }

    public static PropuestaWhatsappId nuevo() {
        return new PropuestaWhatsappId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
