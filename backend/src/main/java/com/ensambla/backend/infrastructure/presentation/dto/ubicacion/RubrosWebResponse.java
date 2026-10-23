package com.ensambla.backend.infrastructure.presentation.dto.ubicacion;

import java.util.List;

public record RubrosWebResponse(List<RubroResponse> rubros) {

    public record RubroResponse(String codigo, String nombre) {
    }
}
