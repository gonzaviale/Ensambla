package com.ensambla.backend.infrastructure.presentation.dto.inventario;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

public record StockResponse(RegistroId registroId, String producto, BigDecimal stock) {
}
