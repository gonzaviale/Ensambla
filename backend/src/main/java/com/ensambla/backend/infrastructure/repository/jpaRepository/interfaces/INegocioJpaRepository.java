package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface INegocioJpaRepository extends JpaRepository<NegocioEntity, UUID> {
}
