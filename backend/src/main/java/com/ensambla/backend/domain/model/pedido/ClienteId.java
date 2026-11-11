package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un cliente que pide por WhatsApp. */
public record ClienteId(UUID valor) implements Identificador {

    public ClienteId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static ClienteId de(UUID valor) {
        return new ClienteId(valor);
    }

    public static ClienteId de(String valor) {
        return new ClienteId(UUID.fromString(valor));
    }

    public static ClienteId nuevo() {
        return new ClienteId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
