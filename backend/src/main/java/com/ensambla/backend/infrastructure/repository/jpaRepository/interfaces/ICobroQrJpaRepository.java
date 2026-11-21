package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.CobroQrEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ICobroQrJpaRepository extends JpaRepository<CobroQrEntity, UUID> {

    Optional<CobroQrEntity> findByIdAndNegocioId(UUID id, UUID negocioId);
}
