package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.VentaCajaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IVentaCajaJpaRepository extends JpaRepository<VentaCajaEntity, UUID> {

    Optional<VentaCajaEntity> findByIdAndNegocioId(UUID id, UUID negocioId);
}
