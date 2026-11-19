package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.Identificador;

import java.util.Objects;
import java.util.UUID;

/** Identificador de la respuesta a un mini-objetivo manual. */
public record RespuestaMiniObjetivoId(UUID valor) implements Identificador {

    public RespuestaMiniObjetivoId {
        Objects.requireNonNull(valor, "El id de respuesta de mini-objetivo es obligatorio");
    }

    public static RespuestaMiniObjetivoId de(UUID valor) {
        return new RespuestaMiniObjetivoId(valor);
    }

    public static RespuestaMiniObjetivoId de(String valor) {
        return new RespuestaMiniObjetivoId(UUID.fromString(valor));
    }

    public static RespuestaMiniObjetivoId nuevo() {
        return new RespuestaMiniObjetivoId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
