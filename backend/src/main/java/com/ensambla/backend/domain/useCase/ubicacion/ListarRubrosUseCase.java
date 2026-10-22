package com.ensambla.backend.domain.useCase.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse;
import com.ensambla.backend.domain.mapper.ubicacion.ListarRubrosMapper;
import com.ensambla.backend.domain.service.ubicacion.RubroService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarRubrosUseCase {

    private final RubroService rubroService;
    private final ListarRubrosMapper listarRubrosMapper;

    public ListarRubrosResponse execute() {
        return listarRubrosMapper.toResponse(rubroService.listar());
    }
}
