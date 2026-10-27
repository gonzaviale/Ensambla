package com.ensambla.backend.domain.useCase.ubicacion.listarRubros;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;

import java.util.List;

public class ListarRubros {

    public ListarRubrosResponse execute() {
        return ListarRubrosResponse.de(List.of(RubroNegocio.values()));
    }
}
