package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import java.math.BigDecimal;
import java.util.UUID;

public record StockItem(UUID registroId, String producto, BigDecimal stock) {
}
