package com.ensambla.backend.infrastructure.presentation.dto.lote;

import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;

import java.time.OffsetDateTime;
import java.util.List;

public record LoteResponse(
        LoteId id,
        String estado,
        OffsetDateTime creadoEn,
        List<FotoResponse> fotos
) {

    public record FotoResponse(FotoId id, Short posicion, String tipoContenido, Long tamanioBytes) {
    }
}
