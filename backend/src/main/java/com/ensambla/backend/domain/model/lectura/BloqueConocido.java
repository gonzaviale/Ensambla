package com.ensambla.backend.domain.model.lectura;

import java.util.List;
import java.util.UUID;

/** Bloque que el negocio ya tiene; se le pasa a la IA para que sume las filas ahí en vez de crear uno nuevo. */
public record BloqueConocido(UUID id, String nombre, List<String> campos) {

    public BloqueConocido {
        campos = List.copyOf(campos);
    }
}
