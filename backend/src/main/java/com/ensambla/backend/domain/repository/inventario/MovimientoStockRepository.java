package com.ensambla.backend.domain.repository.inventario;

import com.ensambla.backend.domain.model.inventario.MovimientoStock;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface MovimientoStockRepository {

    MovimientoStock save(MovimientoStock movimiento);

    /** Entradas menos salidas de cada producto; los que no tienen movimientos no vienen. */
    Map<UUID, BigDecimal> stockDe(Collection<UUID> registroIds, UUID negocioId);

    /**
     * Los últimos movimientos de los productos de un bloque, los más nuevos primero; con {@code registroId} sólo los de
     * ese producto.
     */
    List<MovimientoStock> findRecientes(UUID bloqueId, UUID negocioId, UUID registroId, int limite);
}
