package com.ensambla.backend.infrastructure.presentation.dto.orden;

import java.math.BigDecimal;

public record ItemOrdenResponse(String nombre, BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {
}
