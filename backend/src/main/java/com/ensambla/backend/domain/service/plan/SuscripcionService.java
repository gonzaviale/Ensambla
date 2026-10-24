package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import lombok.RequiredArgsConstructor;

/** Las lecturas de suscripción y catálogo que comparten los casos de uso de planes y pagos. */
@RequiredArgsConstructor
public class SuscripcionService {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    public Suscripcion obtener(NegocioId negocioId) {
        return suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }

    public Plan obtenerPlan(CodigoPlan codigo) {
        return planRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }
}
