package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una orden de compra. */
public record OrdenCompraId(UUID valor) implements Identificador {

    public OrdenCompraId {
        Objects.requireNonNull(valor, "El id es obligatorio");
    }

    public static OrdenCompraId de(UUID valor) {
        return new OrdenCompraId(valor);
    }

    public static OrdenCompraId de(String valor) {
        return new OrdenCompraId(UUID.fromString(valor));
    }

    public static OrdenCompraId nuevo() {
        return new OrdenCompraId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
