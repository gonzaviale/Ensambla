package com.ensambla.backend.domain.dto.negocio;

import java.math.BigDecimal;

public record UbicacionNegocioItem(String direccion, String localidad, BigDecimal latitud, BigDecimal longitud) {
}
