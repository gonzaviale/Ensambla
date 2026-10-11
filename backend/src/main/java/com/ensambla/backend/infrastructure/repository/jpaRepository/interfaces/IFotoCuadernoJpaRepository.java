package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.FotoCuadernoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IFotoCuadernoJpaRepository extends JpaRepository<FotoCuadernoEntity, UUID> {

    /** Fotos del lote en el orden en que se subieron, filtradas también por negocio. */
    List<FotoCuadernoEntity> findAllByLoteIdAndNegocioIdOrderByPosicionAsc(UUID loteId, UUID negocioId);
}
