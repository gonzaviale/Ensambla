package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.exception.RestriccionPlanException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PruebaPremiumService {

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final PlanRepository planRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    @Transactional
    public PruebaPremium iniciar(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        if (pruebaPremiumRepository.findByNegocioId(negocioId).isPresent()) {
            throw new RestriccionPlanException("PRUEBA_UTILIZADA", "Este negocio ya utilizó su prueba premium.");
        }
        if (renovacionPremiumRepository.existeAbierta(negocioId)) {
            throw new RestriccionPlanException("RENOVACION_EN_CURSO",
                    "Tenés una contratación en curso. Cancelala antes de iniciar una prueba.");
        }
        Plan premium = obtenerPlan(CodigoPlan.PREMIUM);
        premium.validarLimitesConfigurados();
        if (suscripcion.estaVigente(ahora) && premium.getId().equals(suscripcion.getPlanId())) {
            throw new RestriccionPlanException("PREMIUM_ACTIVO", "Tu negocio ya tiene premium activo.");
        }
        PruebaPremium prueba = pruebaPremiumRepository.save(PruebaPremium.nueva(negocioId, ahora));
        suscripcion.setPlanId(premium.getId());
        suscripcion.setEstado(EstadoSuscripcion.PRUEBA);
        suscripcion.setInicioEn(prueba.inicioEn());
        suscripcion.setFinEn(prueba.finEn());
        suscripcion.setActualizadoEn(ahora);
        suscripcionRepository.save(suscripcion);
        return prueba;
    }

    public List<UUID> listarVencidas(OffsetDateTime ahora) {
        return pruebaPremiumRepository.findNegociosVencidos(ahora);
    }

    @Transactional
    public void vencer(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Suscripcion suscripcion = obtenerSuscripcion(negocioId);
        // Se vuelve a comprobar luego del bloqueo: un pago puede haberla activado desde la selección del job.
        if (suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && !suscripcion.getFinEn().isAfter(ahora)) {
            suscripcion.setPlanId(obtenerPlan(CodigoPlan.GRATUITO).getId());
            suscripcion.setEstado(EstadoSuscripcion.ACTIVA);
            suscripcion.setInicioEn(ahora);
            suscripcion.setFinEn(null);
            suscripcion.setActualizadoEn(ahora);
            suscripcionRepository.save(suscripcion);
        }
    }

    private Suscripcion obtenerSuscripcion(UUID negocioId) {
        return suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
    }

    private Plan obtenerPlan(CodigoPlan codigo) {
        return planRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }
}
