package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RecetaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IRecetaJpaRepository extends JpaRepository<RecetaEntity, UUID> {

    Optional<RecetaEntity> findByProductoIdAndNegocioId(UUID productoId, UUID negocioId);

    void deleteByProductoIdAndNegocioId(UUID productoId, UUID negocioId);
}
