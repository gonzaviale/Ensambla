package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de la conversación de un cliente con Sami. */
public record ConversacionClienteId(UUID valor) implements Identificador {

    public ConversacionClienteId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static ConversacionClienteId de(UUID valor) {
        return new ConversacionClienteId(valor);
    }

    public static ConversacionClienteId de(String valor) {
        return new ConversacionClienteId(UUID.fromString(valor));
    }

    public static ConversacionClienteId nuevo() {
        return new ConversacionClienteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
