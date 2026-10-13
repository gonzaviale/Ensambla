package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.MovimientoStockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IMovimientoStockJpaRepository extends JpaRepository<MovimientoStockEntity, UUID> {

    /** Entradas menos salidas por producto, sólo de los movimientos del negocio; los productos sin movimientos no salen. */
    @Query(value = """
            SELECT m.registro_id AS registro,
                   SUM(CASE m.tipo WHEN 'ENTRADA' THEN m.cantidad ELSE -m.cantidad END) AS stock
            FROM movimiento_stock m
            WHERE m.negocio_id = :negocioId
              AND m.registro_id IN (:registroIds)
            GROUP BY m.registro_id
            """, nativeQuery = true)
    List<StockPorRegistro> stockDe(@Param("negocioId") UUID negocioId,
                                   @Param("registroIds") Collection<UUID> registroIds);

    /**
     * Últimos movimientos de los productos del bloque (el bloque se resuelve por el registro), los más nuevos primero;
     * con {@code registroId} sólo los de ese producto.
     */
    @Query(value = """
            SELECT m.*
            FROM movimiento_stock m
            JOIN registro r ON r.negocio_id = m.negocio_id AND r.id = m.registro_id
            WHERE m.negocio_id = :negocioId
              AND r.bloque_id = :bloqueId
              AND (CAST(:registroId AS text) IS NULL OR m.registro_id = CAST(CAST(:registroId AS text) AS uuid))
            ORDER BY m.creado_en DESC, m.id DESC
            LIMIT :limite
            """, nativeQuery = true)
    List<MovimientoStockEntity> findRecientes(@Param("negocioId") UUID negocioId,
                                              @Param("bloqueId") UUID bloqueId,
                                              @Param("registroId") String registroId,
                                              @Param("limite") int limite);

    interface StockPorRegistro {

        UUID getRegistro();

        BigDecimal getStock();
    }
}
