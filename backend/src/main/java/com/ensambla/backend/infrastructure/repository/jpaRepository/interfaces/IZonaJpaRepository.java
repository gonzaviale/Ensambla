package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ZonaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IZonaJpaRepository extends JpaRepository<ZonaEntity, UUID> {
}
