package com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IPlanJpaRepository extends JpaRepository<PlanEntity, UUID> {

    /** El catálogo es global y el código identifica un único plan. */
    Optional<PlanEntity> findByCodigo(CodigoPlan codigo);
}
