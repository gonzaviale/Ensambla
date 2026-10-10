package com.ensambla.backend.domain.mapper.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.model.bloque.Bloque;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CrearBloqueMapper {

    private final BloqueItemMapper bloqueItemMapper;

    public BloqueItem toResponse(Bloque bloque) {
        return bloqueItemMapper.toResponse(bloque);
    }
}
