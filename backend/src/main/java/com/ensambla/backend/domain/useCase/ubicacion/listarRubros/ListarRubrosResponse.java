package com.ensambla.backend.domain.useCase.ubicacion.listarRubros;

import com.ensambla.backend.domain.model.negocio.RubroNegocio;

import java.util.List;

public record ListarRubrosResponse(List<RubroItem> rubros) {

    public record RubroItem(String codigo, String nombre) {
    }

    public static ListarRubrosResponse de(List<RubroNegocio> rubros) {
        return new ListarRubrosResponse(rubros.stream()
                .map(rubro -> new RubroItem(rubro.name(), rubro.getNombre()))
                .toList());
    }
}
