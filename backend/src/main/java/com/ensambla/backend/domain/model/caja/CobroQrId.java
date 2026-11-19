package com.ensambla.backend.domain.model.caja;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

public record CobroQrId(UUID valor) implements Identificador {

    public CobroQrId {
        Objects.requireNonNull(valor, "El id del cobro con QR es obligatorio");
    }

    public static CobroQrId de(UUID valor) {
        return new CobroQrId(valor);
    }

    public static CobroQrId de(String valor) {
        return de(UUID.fromString(valor));
    }

    public static CobroQrId nuevo() {
        return de(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
