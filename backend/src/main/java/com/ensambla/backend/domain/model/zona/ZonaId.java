package com.ensambla.backend.domain.model.zona;

import com.ensambla.backend.domain.model.common.Identificador;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Identificador de una zona. */
public record ZonaId(UUID valor) implements Identificador {

    public ZonaId {
        Objects.requireNonNull(valor, "El id de zona es obligatorio");
    }

    public static ZonaId de(UUID valor) {
        return new ZonaId(valor);
    }

    public static ZonaId de(String valor) {
        return new ZonaId(UUID.fromString(valor));
    }

    /** Id estable para un lugar suelto del mapa: las mismas coordenadas dan siempre el mismo id. */
    public static ZonaId deClave(String clave) {
        return new ZonaId(UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8)));
    }

    public static ZonaId nuevo() {
        return new ZonaId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
