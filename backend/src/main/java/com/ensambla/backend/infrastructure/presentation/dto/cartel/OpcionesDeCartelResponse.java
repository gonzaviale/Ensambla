package com.ensambla.backend.infrastructure.presentation.dto.cartel;

import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;

import java.util.List;

public record OpcionesDeCartelResponse(List<OpcionResponse> opciones) {

    public record OpcionResponse(VarianteDeComposicion variante, String dibujo) {
    }
}
