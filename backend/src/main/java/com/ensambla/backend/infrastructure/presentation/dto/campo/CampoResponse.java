package com.ensambla.backend.infrastructure.presentation.dto.campo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record CampoResponse(
        UUID id,
        UUID bloqueId,
        String nombre,
        String tipo,
        boolean obligatorio,
        String valorInicial,
        List<String> opciones,
        UUID bloqueRelacionadoId,
        Short posicion,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
