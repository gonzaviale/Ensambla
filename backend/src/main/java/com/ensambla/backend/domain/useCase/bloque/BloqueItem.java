package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.model.bloque.Bloque;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BloqueItem(
        UUID id,
        UUID proyectoId,
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
