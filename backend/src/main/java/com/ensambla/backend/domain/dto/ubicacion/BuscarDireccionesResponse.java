package com.ensambla.backend.domain.dto.ubicacion;

import java.math.BigDecimal;
import java.util.List;

public record BuscarDireccionesResponse(List<DireccionItem> resultados) {

    public record DireccionItem(String direccion, BigDecimal latitud, BigDecimal longitud) {
    }
}
