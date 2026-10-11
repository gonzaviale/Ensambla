package com.ensambla.backend.domain.mapper.lote;

import com.ensambla.backend.domain.dto.lote.LoteItem;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerLoteMapper {

    private final LoteItemMapper loteItemMapper;

    public LoteItem toResponse(LoteSubida lote) {
        return loteItemMapper.toResponse(lote);
    }
}
