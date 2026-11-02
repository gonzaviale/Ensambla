package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.HistorialPrecioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface IHistorialPrecioJpaRepository extends JpaRepository<HistorialPrecioEntity, UUID> {

    List<HistorialPrecioEntity> findByNegocioIdAndCambiadoEnGreaterThanEqualOrderByCambiadoEnAsc(
            UUID negocioId, OffsetDateTime desde);
}
