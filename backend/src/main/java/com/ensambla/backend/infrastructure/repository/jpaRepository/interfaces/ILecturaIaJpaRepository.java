package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.LecturaIaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ILecturaIaJpaRepository extends JpaRepository<LecturaIaEntity, UUID> {
}
