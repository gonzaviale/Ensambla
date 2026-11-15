package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.KitDeMarcaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IKitDeMarcaJpaRepository extends JpaRepository<KitDeMarcaEntity, UUID> {

    /** Aislamiento por negocio: el kit se busca siempre por el negocio de la sesión. */
    Optional<KitDeMarcaEntity> findByNegocioId(UUID negocioId);
}
