package com.ensambla.backend.domain.mapper.receta;

import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ObtenerRecetaMapper {

    private final RecetaItemMapper recetaItemMapper;

    public RecetaItem toResponse(RecetaCosteada receta) {
        return recetaItemMapper.toItem(receta);
    }
}
