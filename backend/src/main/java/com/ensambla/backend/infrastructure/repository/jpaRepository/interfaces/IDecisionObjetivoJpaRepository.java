package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.infrastructure.repository.entity.DecisionObjetivoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface IDecisionObjetivoJpaRepository extends JpaRepository<DecisionObjetivoEntity, UUID> {

    /** Nunca lista decisiones de otro negocio. De la más nueva a la más vieja. */
    List<DecisionObjetivoEntity> findAllByObjetivoIdAndNegocioIdOrderByTomadaEnDescIdDesc(
            UUID objetivoId, UUID negocioId);
}
