package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IFotoCuadernoJpaRepository extends JpaRepository<FotoCuadernoEntity, UUID> {
}
