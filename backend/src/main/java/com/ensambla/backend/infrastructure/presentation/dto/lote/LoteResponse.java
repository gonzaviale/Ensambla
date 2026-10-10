package com.ensambla.backend.infrastructure.presentation.dto.lote;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoteResponse(
        UUID id,
        String estado,
        OffsetDateTime creadoEn,
        List<FotoResponse> fotos
) {

    public record FotoResponse(
            UUID id,
            Short posicion,
            String tipoContenido,
            Long tamanioBytes,
            String url,
            Instant urlExpiraEn
    ) {
    }
}
