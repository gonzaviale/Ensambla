package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;

public record BloqueItem(
        BloqueId id,
        ProyectoId proyectoId,
        String nombre,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {

    public static BloqueItem de(Bloque bloque) {
        return new BloqueItem(
                bloque.getId(),
                bloque.getProyectoId(),
                bloque.getNombre(),
                bloque.getPosicion(),
                bloque.getCreadoEn(),
                bloque.getActualizadoEn());
    }
}
