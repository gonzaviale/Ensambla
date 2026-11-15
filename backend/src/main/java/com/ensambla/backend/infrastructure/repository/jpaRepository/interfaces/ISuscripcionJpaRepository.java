package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ISuscripcionJpaRepository extends JpaRepository<SuscripcionEntity, UUID> {

    /** Toda consulta de la suscripción actual se restringe al negocio resuelto desde la sesión. */
    Optional<SuscripcionEntity> findByNegocioId(UUID negocioId);
}
