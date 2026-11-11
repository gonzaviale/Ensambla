package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un mensaje de un cliente en la cola de atención. */
public record MensajeClienteId(UUID valor) implements Identificador {

    public MensajeClienteId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static MensajeClienteId de(UUID valor) {
        return new MensajeClienteId(valor);
    }

    public static MensajeClienteId de(String valor) {
        return new MensajeClienteId(UUID.fromString(valor));
    }

    public static MensajeClienteId nuevo() {
        return new MensajeClienteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
