package com.ensambla.backend.domain.service.inventario;

import com.ensambla.backend.domain.repository.inventario.MovimientoStockRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RequiredArgsConstructor
public class InventarioService {

    private final MovimientoStockRepository movimientoStockRepository;

    /** Stock de cada producto pedido, en el orden pedido; sin movimientos es cero. */
    public Map<UUID, BigDecimal> stockDe(Collection<UUID> registroIds, UUID negocioId) {
        Map<UUID, BigDecimal> existentes = movimientoStockRepository.stockDe(registroIds, negocioId);
        Map<UUID, BigDecimal> stock = new LinkedHashMap<>();
        registroIds.forEach(id -> stock.put(id, existentes.getOrDefault(id, BigDecimal.ZERO)));
        return stock;
    }
}
