package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador del resumen del cierre de una semana de un objetivo. */
public record ResumenSemanalId(UUID valor) implements Identificador {

    public ResumenSemanalId {
        Objects.requireNonNull(valor, "El id de resumen semanal de objetivo es obligatorio");
    }

    public static ResumenSemanalId de(UUID valor) {
        return new ResumenSemanalId(valor);
    }

    public static ResumenSemanalId de(String valor) {
        return new ResumenSemanalId(UUID.fromString(valor));
    }

    public static ResumenSemanalId nuevo() {
        return new ResumenSemanalId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
