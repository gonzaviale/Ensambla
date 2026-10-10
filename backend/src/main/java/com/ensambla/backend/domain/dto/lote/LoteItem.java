package com.ensambla.backend.domain.dto.lote;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoteItem(
        UUID id,
        String estado,
        OffsetDateTime creadoEn,
        List<FotoItem> fotos
) {

    public record FotoItem(UUID id, Short posicion, String tipoContenido, Long tamanioBytes) {
    }
}
