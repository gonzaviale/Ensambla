package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.VinculoWhatsappEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IVinculoWhatsappJpaRepository extends JpaRepository<VinculoWhatsappEntity, UUID> {

    Optional<VinculoWhatsappEntity> findByNegocioId(UUID negocioId);

    Optional<VinculoWhatsappEntity> findByCodigo(String codigo);

    Optional<VinculoWhatsappEntity> findByTelefono(String telefono);

    boolean existsByCodigo(String codigo);
}
