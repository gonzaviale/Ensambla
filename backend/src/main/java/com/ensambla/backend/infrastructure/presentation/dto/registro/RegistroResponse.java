package com.ensambla.backend.infrastructure.presentation.dto.registro;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.time.OffsetDateTime;
import java.util.Map;

public record RegistroResponse(
        RegistroId id,
        BloqueId bloqueId,
        Map<String, Object> valores,
        FotoId fotoOrigenId,
        OffsetDateTime creadoEn,
        OffsetDateTime actualizadoEn
) {
}
