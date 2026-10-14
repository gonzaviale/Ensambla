package com.ensambla.backend.domain.mapper.receta;

import com.ensambla.backend.domain.dto.receta.GuardarRecetaRequest;
import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.model.receta.LineaReceta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class GuardarRecetaMapper {

    private final RecetaItemMapper recetaItemMapper;

    public List<LineaReceta> toLineas(GuardarRecetaRequest request) {
        return request.lineas().stream()
                .map(linea -> new LineaReceta(linea.insumoId(), linea.cantidad(), linea.unidad()))
                .toList();
    }

    public RecetaItem toResponse(RecetaCosteada receta) {
        return recetaItemMapper.toItem(receta);
    }
}
