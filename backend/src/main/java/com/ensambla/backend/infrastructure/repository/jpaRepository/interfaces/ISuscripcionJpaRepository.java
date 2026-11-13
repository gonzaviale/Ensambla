package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ISuscripcionJpaRepository extends JpaRepository<SuscripcionEntity, UUID> {

    /** La suscripción es de la cuenta de la sesión: nunca se pide por un id que venga del cliente. */
    Optional<SuscripcionEntity> findByUsuarioId(UUID usuarioId);
}
