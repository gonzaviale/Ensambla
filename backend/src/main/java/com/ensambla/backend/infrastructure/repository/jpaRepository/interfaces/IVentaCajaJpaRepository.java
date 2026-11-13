package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.VentaCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IVentaCajaJpaRepository extends JpaRepository<VentaCajaEntity, UUID> {

    Optional<VentaCajaEntity> findByIdAndNegocioId(UUID id, UUID negocioId);

    List<VentaCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdAndCierreIdIsNullOrderByCreadoEnAscIdAsc(
            UUID negocioId, UUID proyectoId, UUID inventarioId);

    List<VentaCajaEntity> findByNegocioIdAndProyectoIdAndInventarioIdAndCreadoEnGreaterThanEqualAndCreadoEnLessThanOrderByCreadoEnAscIdAsc(
            UUID negocioId, UUID proyectoId, UUID inventarioId, OffsetDateTime desde, OffsetDateTime hasta);

    /** El negocio ya está bloqueado por el caso de uso, igual que al cobrar. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update VentaCajaEntity v set v.cierreId = :cierreId where v.negocioId = :negocioId and v.id in :ventas and v.cierreId is null")
    void marcarCerradas(UUID negocioId, UUID cierreId, List<UUID> ventas);
}
