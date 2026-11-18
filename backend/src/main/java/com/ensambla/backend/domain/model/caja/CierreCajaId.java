package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

public record CierreCajaId(UUID valor) implements Identificador {

    public CierreCajaId {
        Objects.requireNonNull(valor, "El id de cierre es obligatorio");
    }

    public static CierreCajaId de(UUID valor) {
        return new CierreCajaId(valor);
    }

    public static CierreCajaId de(String valor) {
        return de(UUID.fromString(valor));
    }

    public static CierreCajaId nuevo() {
        return de(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
