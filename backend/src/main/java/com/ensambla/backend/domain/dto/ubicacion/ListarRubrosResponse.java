package com.ensambla.backend.domain.dto.ubicacion;

import java.util.List;

public record ListarRubrosResponse(List<RubroItem> rubros) {

    public record RubroItem(String codigo, String nombre) {
    }
}
