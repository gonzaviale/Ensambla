package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

public record VentaCajaId(UUID valor) implements Identificador {

    public VentaCajaId {
        Objects.requireNonNull(valor, "El id de venta es obligatorio");
    }

    public static VentaCajaId de(UUID valor) {
        return new VentaCajaId(valor);
    }

    public static VentaCajaId de(String valor) {
        return de(UUID.fromString(valor));
    }

    public static VentaCajaId nuevo() {
        return de(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
