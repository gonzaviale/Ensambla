package com.ensambla.backend.domain.mapper.ubicacion;

import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse;
import com.ensambla.backend.domain.dto.ubicacion.ListarRubrosResponse.RubroItem;
import com.ensambla.backend.domain.model.enums.RubroNegocio;

import java.util.List;

public class ListarRubrosMapper {

    public ListarRubrosResponse toResponse(List<RubroNegocio> rubros) {
        return new ListarRubrosResponse(rubros.stream()
                .map(rubro -> new RubroItem(rubro.name(), rubro.getNombre()))
                .toList());
    }
}
