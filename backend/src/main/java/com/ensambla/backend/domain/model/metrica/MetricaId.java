package com.ensambla.backend.domain.model.metrica;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de una métrica guardada en un cuaderno. */
public record MetricaId(UUID valor) implements Identificador {

    public MetricaId {
        Objects.requireNonNull(valor, "El id de métrica es obligatorio");
    }

    public static MetricaId de(UUID valor) {
        return new MetricaId(valor);
    }

    public static MetricaId de(String valor) {
        return new MetricaId(UUID.fromString(valor));
    }

    public static MetricaId nuevo() {
        return new MetricaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
