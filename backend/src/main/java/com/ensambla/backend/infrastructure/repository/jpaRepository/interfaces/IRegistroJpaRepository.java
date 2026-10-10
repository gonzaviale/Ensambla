package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RegistroEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IRegistroJpaRepository extends JpaRepository<RegistroEntity, UUID> {
}
