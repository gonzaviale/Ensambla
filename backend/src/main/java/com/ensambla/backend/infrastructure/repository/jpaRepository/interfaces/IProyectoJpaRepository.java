package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.ProyectoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IProyectoJpaRepository extends JpaRepository<ProyectoEntity, UUID> {
}
