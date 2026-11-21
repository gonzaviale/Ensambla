package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

public record CobroCajaId(UUID valor) implements Identificador {

    public CobroCajaId {
        Objects.requireNonNull(valor, "El id de cobro es obligatorio");
    }

    public static CobroCajaId de(UUID valor) {
        return new CobroCajaId(valor);
    }

    public static CobroCajaId de(String valor) {
        return de(UUID.fromString(valor));
    }

    public static CobroCajaId nuevo() {
        return de(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
