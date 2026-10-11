package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.NegocioEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface INegocioJpaRepository extends JpaRepository<NegocioEntity, UUID> {

    /** MVP2: negocio del dueño autenticado; el selector de negocio se implementa en T-061. */
    Optional<NegocioEntity> findFirstByDuenioIdOrderByCreadoEnAscIdAsc(UUID duenioId);
}
