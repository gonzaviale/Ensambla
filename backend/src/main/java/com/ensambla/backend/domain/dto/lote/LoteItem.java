package com.ensambla.backend.domain.dto.lote;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LoteItem(
        UUID id,
        String estado,
        OffsetDateTime creadoEn,
        List<FotoItem> fotos
) {

    /**
     * @param url        URL firmada para ver la foto; {@code null} al subir el lote
     * @param urlExpiraEn cuándo vence {@code url}; {@code null} si no hay URL
     */
    public record FotoItem(
            UUID id,
            Short posicion,
            String tipoContenido,
            Long tamanioBytes,
            String url,
            Instant urlExpiraEn
    ) {
    }
}
