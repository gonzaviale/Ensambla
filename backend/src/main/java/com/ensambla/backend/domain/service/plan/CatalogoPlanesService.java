package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CatalogoPlanesService {

    private final PlanRepository planRepository;

    public List<Plan> listar() {
        return List.of(obtener(CodigoPlan.GRATUITO), obtener(CodigoPlan.PREMIUM));
    }

    private Plan obtener(CodigoPlan codigo) {
        return planRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }
}
