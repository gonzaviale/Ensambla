package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.SuscripcionRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
public class ControlPlanService {

    private final PlanRepository planRepository;

    private final SuscripcionRepository suscripcionRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final TransaccionPort transaccionPort;

    public void validarAltaProyecto(UUID negocioId, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            plan.validarProyectos(usoPlanRepository.contarProyectos(negocioId));
        });
    }

    public void validarAltaRegistros(UUID negocioId, long cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            plan.validarRegistros(usoPlanRepository.contarRegistros(negocioId), cantidad);
        });
    }

    public void validarPaginasIa(UUID negocioId, int cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            Plan plan = vigente(negocioId, ahora);
            Optional<PruebaPremium> prueba = pruebaVigente(negocioId, ahora);
            if (prueba.isPresent()) {
                prueba.get().validarPaginas(cantidad);
                return;
            }
            LocalDate mes = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
            plan.validarPaginasIa(usoPlanRepository.paginasEnPeriodo(negocioId, mes, mes.plusMonths(1)), cantidad);
        });
    }

    public void validarFuncion(UUID negocioId, FuncionPlan funcion, OffsetDateTime ahora) {
        vigente(negocioId, ahora).validarFuncion(funcion);
    }

    public boolean esPremium(UUID negocioId, OffsetDateTime ahora) {
        return vigente(negocioId, ahora).getCodigo() == CodigoPlan.PREMIUM;
    }

    /** Se cobra al admitir un intento. También cuentan errores y timeouts, sin duplicar al finalizar el lote. */
    public void registrarPaginasPrueba(UUID negocioId, int cantidad, OffsetDateTime ahora) {
        transaccionPort.ejecutar(() -> {
            usoPlanRepository.bloquearNegocio(negocioId);
            pruebaVigente(negocioId, ahora)
                    .ifPresent(prueba -> pruebaPremiumRepository.save(prueba.consumir(cantidad)));
        });
    }

    private Optional<PruebaPremium> pruebaVigente(UUID negocioId, OffsetDateTime ahora) {
        Suscripcion suscripcion = suscripcionRepository.findByNegocioId(negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Suscripción no encontrada"));
        if (suscripcion.getEstado() == EstadoSuscripcion.PRUEBA && suscripcion.estaVigente(ahora)) {
            return Optional.of(pruebaPremiumRepository.findByNegocioId(negocioId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prueba no encontrada")));
        }
        return Optional.empty();
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
