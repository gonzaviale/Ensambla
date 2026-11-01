package com.ensambla.backend.domain.useCase.inventario.consultarStock;

import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

public record StockItem(RegistroId registroId, String producto, BigDecimal stock) {
}
