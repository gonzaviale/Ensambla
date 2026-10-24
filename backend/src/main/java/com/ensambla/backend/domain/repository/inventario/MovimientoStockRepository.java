package com.ensambla.backend.domain.repository.inventario;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.inventario.MovimientoStock;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MovimientoStockRepository {

    MovimientoStock save(MovimientoStock movimiento);

    /** Entradas menos salidas de cada producto; los que no tienen movimientos no vienen. */
    Map<RegistroId, BigDecimal> stockDe(Collection<RegistroId> registroIds, NegocioId negocioId);

    /**
     * Los últimos movimientos de los productos de un bloque, los más nuevos primero; con {@code registroId} sólo los de
     * ese producto.
     */
    List<MovimientoStock> findRecientes(BloqueId bloqueId, NegocioId negocioId, RegistroId registroId, int limite);
}
