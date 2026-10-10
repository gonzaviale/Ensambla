package com.ensambla.backend.infrastructure.presentation.dto.registro;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record RegistroResponse(
        UUID id,
        UUID bloqueId,
        Map<String, Object> valores,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
