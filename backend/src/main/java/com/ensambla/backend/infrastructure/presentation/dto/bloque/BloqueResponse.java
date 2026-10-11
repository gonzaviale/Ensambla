package com.ensambla.backend.infrastructure.presentation.dto.bloque;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BloqueResponse(
        UUID id,
        UUID proyectoId,
        String nombre,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
