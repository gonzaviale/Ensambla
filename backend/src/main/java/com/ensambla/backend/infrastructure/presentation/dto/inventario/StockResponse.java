package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import java.math.BigDecimal;
import java.util.UUID;

public record StockResponse(UUID registroId, String producto, BigDecimal stock) {
}
