package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PlanId;

import java.util.Optional;

public interface PlanRepository {

    Optional<Plan> findByCodigo(CodigoPlan codigo);

    Optional<Plan> findById(PlanId planId);
}
