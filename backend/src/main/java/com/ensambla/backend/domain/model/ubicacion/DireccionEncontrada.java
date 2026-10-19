package com.ensambla.backend.domain.model.ubicacion;

import java.math.BigDecimal;

public record DireccionEncontrada(String direccion, BigDecimal latitud, BigDecimal longitud) {
}
