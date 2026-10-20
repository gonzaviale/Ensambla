package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ControlPlanService {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    @Transactional
    public void validarAltaProyecto(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Plan plan = vigente(negocioId, ahora);
        plan.validarProyectos(usoPlanRepository.contarProyectos(negocioId));
    }

    @Transactional
    public void validarAltaRegistros(UUID negocioId, long cantidad, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Plan plan = vigente(negocioId, ahora);
        plan.validarRegistros(usoPlanRepository.contarRegistros(negocioId), cantidad);
    }

    @Transactional
    public void validarPaginasIa(UUID negocioId, int cantidad, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Plan plan = vigente(negocioId, ahora);
        LocalDate mes = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        plan.validarPaginasIa(usoPlanRepository.paginasEnPeriodo(negocioId, mes, mes.plusMonths(1)), cantidad);
    }

    public void validarFuncion(UUID negocioId, FuncionPlan funcion, OffsetDateTime ahora) {
        vigente(negocioId, ahora).validarFuncion(funcion);
    }

    private Plan vigente(UUID negocioId, OffsetDateTime ahora) {
        Suscripcion suscripcion = suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        Plan plan = suscripcion.estaVigente(ahora)
                ? planRepository.findById(suscripcion.getPlanId())
                    .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"))
                : planRepository.findByCodigo(CodigoPlan.GRATUITO)
                    .orElseThrow(() -> new ResourceNotFoundException("Plan gratuito no encontrado"));
        plan.validarLimitesConfigurados();
        return plan;
    }
}
