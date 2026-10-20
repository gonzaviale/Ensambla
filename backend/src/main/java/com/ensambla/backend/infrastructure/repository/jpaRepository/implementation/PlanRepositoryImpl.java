package com.ensambla.backend.infrastructure.repository.jpaRepository.implementation;

import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.infrastructure.repository.jpaRepository.interfaces.IPlanJpaRepository;
import com.ensambla.backend.infrastructure.repository.mapper.PlanRepositoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PlanRepositoryImpl implements PlanRepository {

    private final IPlanJpaRepository planJpaRepository;

    private final PlanRepositoryMapper planRepositoryMapper;

    @Override
    public Optional<Plan> findByCodigo(CodigoPlan codigo) {
        return planJpaRepository.findByCodigo(codigo).map(planRepositoryMapper::toDomain);
    }

    @Override
    public Optional<Plan> findById(UUID planId) {
        return planJpaRepository.findById(planId).map(planRepositoryMapper::toDomain);
    }
}
