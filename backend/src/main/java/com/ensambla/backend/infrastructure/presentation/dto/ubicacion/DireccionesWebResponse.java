package com.ensambla.backend.infrastructure.presentation.dto.ubicacion;

import java.math.BigDecimal;
import java.util.List;

public record DireccionesWebResponse(List<DireccionResponse> resultados) {

    public record DireccionResponse(String direccion, BigDecimal latitud, BigDecimal longitud) {
    }
}
