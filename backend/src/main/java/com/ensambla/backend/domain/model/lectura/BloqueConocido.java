package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.model.bloque.BloqueId;

import java.util.List;

/** Bloque que el negocio ya tiene; se le pasa a la IA para que sume las filas ahí en vez de crear uno nuevo. */
public record BloqueConocido(BloqueId id, String nombre, List<CampoContexto> campos) {

    public BloqueConocido {
        campos = List.copyOf(campos);
    }
}
