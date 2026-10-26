package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ILoteSubidaJpaRepository extends JpaRepository<LoteSubidaEntity, UUID> {

    /** Un lote de otro negocio se comporta como inexistente. */
    Optional<LoteSubidaEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    /**
     * Cambia el estado sólo si el lote es del negocio y está en el estado esperado; devuelve cuántos lotes cambió (0 o 1).
     * Vuelca antes lo pendiente: la clave del bloque destino tiene que existir en la base al ejecutarse.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update LoteSubidaEntity l
            set l.estado = :nuevo, l.bloqueDestinoId = :bloqueDestinoId, l.confirmadoEn = :ahora, l.actualizadoEn = :ahora
            where l.id = :id and l.negocioId = :negocioId and l.estado = :esperado
            """)
    int cambiarEstado(@Param("id") UUID id,
                      @Param("negocioId") UUID negocioId,
                      @Param("esperado") EstadoLote esperado,
                      @Param("nuevo") EstadoLote nuevo,
                      @Param("bloqueDestinoId") UUID bloqueDestinoId,
                      @Param("ahora") OffsetDateTime ahora);
}
