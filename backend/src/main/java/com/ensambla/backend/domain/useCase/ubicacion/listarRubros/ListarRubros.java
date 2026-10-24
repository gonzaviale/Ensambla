package com.ensambla.backend.domain.useCase.ubicacion.listarRubros;

import com.ensambla.backend.domain.service.ubicacion.RubroService;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ListarRubros {

    private final RubroService rubroService;

    public ListarRubrosResponse execute() {
        return ListarRubrosResponse.de(rubroService.listar());
    }
}
