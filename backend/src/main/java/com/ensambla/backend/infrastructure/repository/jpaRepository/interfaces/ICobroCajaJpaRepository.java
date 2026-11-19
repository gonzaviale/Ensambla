package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CobroCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICobroCajaJpaRepository extends JpaRepository<CobroCajaEntity, UUID> {

    Optional<CobroCajaEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<CobroCajaEntity> findByNegocioIdAndProyectoIdOrderByCreadoEnAscIdAsc(UUID negocioId, UUID proyectoId);

    List<CobroCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdOrderByCreadoEnAscIdAsc(
            UUID negocioId, UUID proyectoId, UUID inventarioId);

    List<CobroCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(
            UUID negocioId, UUID proyectoId, UUID inventarioId);

    List<CobroCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
            UUID negocioId, UUID proyectoId, UUID inventarioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** El negocio ya está bloqueado por el caso de uso de cierre. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CobroCajaEntity c set c.cierreId = :cierreId where c.negocioId = :negocioId and c.id in :cobros and c.cierreId is null")
    void marcarCerrados(UUID negocioId, UUID cierreId, List<UUID> cobros);
}
