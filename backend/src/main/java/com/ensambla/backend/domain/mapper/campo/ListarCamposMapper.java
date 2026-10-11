package com.ensambla.backend.domain.mapper.campo;

import com.ensambla.backend.domain.dto.campo.CampoItem;
import com.ensambla.backend.domain.model.campo.Campo;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ListarCamposMapper {

    private final CampoItemMapper campoItemMapper;

    public List<CampoItem> toResponse(List<Campo> campos) {
        return campos.stream().map(campoItemMapper::toResponse).toList();
    }
}
