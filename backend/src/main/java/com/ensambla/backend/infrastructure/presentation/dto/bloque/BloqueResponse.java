package com.ensambla.backend.infrastructure.presentation.dto.bloque;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.time.OffsetDateTime;

public record BloqueResponse(
        BloqueId id,
        ProyectoId proyectoId,
        String nombre,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
