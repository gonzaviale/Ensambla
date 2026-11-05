package com.ensambla.backend.infrastructure.presentation.dto.reposicion;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

public record ProductoFaltanteResponse(
        RegistroId id,
        String nombre,
        BigDecimal cantidadActual,
        BigDecimal cantidadAPedir,
        String situacion) {
}
