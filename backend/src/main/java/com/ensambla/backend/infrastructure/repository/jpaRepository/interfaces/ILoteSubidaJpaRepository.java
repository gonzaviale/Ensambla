package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.LoteSubidaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ILoteSubidaJpaRepository extends JpaRepository<LoteSubidaEntity, UUID> {
}
