package com.ensambla.backend.domain.mapper.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.model.bloque.Bloque;

public class BloqueItemMapper {

    public BloqueItem toResponse(Bloque bloque) {
        return new BloqueItem(
                bloque.getId(),
                bloque.getProyectoId(),
                bloque.getNombre(),
                bloque.getPosicion(),
                bloque.getCreadoEn(),
                bloque.getActualizadoEn());
    }
}
