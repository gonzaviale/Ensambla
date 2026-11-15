package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.RespuestaMiniObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IRespuestaMiniObjetivoJpaRepository extends JpaRepository<RespuestaMiniObjetivoEntity, UUID> {

    boolean existsByMiniObjetivoId(UUID miniObjetivoId);

    /** Nunca las respuestas de otro negocio. */
    List<RespuestaMiniObjetivoEntity> findAllByObjetivoIdAndNegocioId(UUID objetivoId, UUID negocioId);
}
