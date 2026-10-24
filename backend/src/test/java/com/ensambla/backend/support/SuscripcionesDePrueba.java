package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

/**
 * Prepara la suscripción de los negocios que los tests de integración insertan a mano, como la deja el registro de
 * una cuenta nueva.
 */
@Component
@RequiredArgsConstructor
public class SuscripcionesDePrueba {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    public Suscripcion asignarGratuito(NegocioId negocioId, OffsetDateTime instante) {
        PlanId planId = planRepository.findByCodigo(CodigoPlan.GRATUITO).orElseThrow().getId();
        return suscripcionRepository.save(Suscripcion.builder()
                .negocioId(negocioId)
                .planId(planId)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(instante)
                .creadoEn(instante)
                .actualizadoEn(instante)
                .build());
    }
}
