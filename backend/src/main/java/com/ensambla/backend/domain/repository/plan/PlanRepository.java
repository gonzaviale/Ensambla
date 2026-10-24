package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;

import java.util.Optional;
import java.util.UUID;

public interface PlanRepository {

    Optional<Plan> findByCodigo(CodigoPlan codigo);

    Optional<Plan> findById(UUID planId);
}
