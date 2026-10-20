package com.ensambla.backend.domain.repository.plan;

import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;

import java.util.Optional;

public interface PlanRepository {

    Optional<Plan> findByCodigo(CodigoPlan codigo);
}
