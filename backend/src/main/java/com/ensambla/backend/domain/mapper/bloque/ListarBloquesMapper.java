package com.ensambla.backend.domain.mapper.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.model.bloque.Bloque;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarBloquesMapper {

    private final BloqueItemMapper bloqueItemMapper;

    public List<BloqueItem> toResponse(List<Bloque> bloques) {
        return bloques.stream().map(bloqueItemMapper::toResponse).toList();
    }
}
