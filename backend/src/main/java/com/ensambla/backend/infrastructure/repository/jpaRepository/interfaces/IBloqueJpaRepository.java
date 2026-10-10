package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.BloqueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IBloqueJpaRepository extends JpaRepository<BloqueEntity, UUID> {

    Optional<BloqueEntity> findByIdAndNegocioId(UUID id, UUID negocioId);
}
