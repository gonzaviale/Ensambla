package com.ensambla.backend.domain.model.inventario;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de un movimiento de stock. */
public record MovimientoStockId(UUID valor) implements Identificador {

    public MovimientoStockId {
        Objects.requireNonNull(valor, "El id de movimiento es obligatorio");
    }

    public static MovimientoStockId de(UUID valor) {
        return new MovimientoStockId(valor);
    }

    public static MovimientoStockId de(String valor) {
        return new MovimientoStockId(UUID.fromString(valor));
    }

    public static MovimientoStockId nuevo() {
        return new MovimientoStockId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
