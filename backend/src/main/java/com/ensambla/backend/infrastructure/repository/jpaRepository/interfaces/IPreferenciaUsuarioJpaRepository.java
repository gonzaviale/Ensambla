package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.PreferenciaUsuarioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IPreferenciaUsuarioJpaRepository extends JpaRepository<PreferenciaUsuarioEntity, UUID> {
}
