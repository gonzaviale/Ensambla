package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import lombok.RequiredArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class SuscripcionService {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final TransaccionPort transaccionPort;

    public Suscripcion asignarGratuito(UUID negocioId, OffsetDateTime instante) {
        return transaccionPort.ejecutar(() -> {
            Plan plan = planRepository.findByCodigo(CodigoPlan.GRATUITO)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan gratuito no encontrado"));
            return suscripcionRepository.save(Suscripcion.builder()
                    .negocioId(negocioId)
                    .planId(plan.getId())
                    .estado(EstadoSuscripcion.ACTIVA)
                    .inicioEn(instante)
                    .creadoEn(instante)
                    .actualizadoEn(instante)
                    .build());
        });
    }

    public Suscripcion obtener(UUID negocioId) {
        return suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }
}
