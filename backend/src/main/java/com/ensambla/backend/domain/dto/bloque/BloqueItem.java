package com.ensambla.backend.domain.dto.bloque;

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
}
