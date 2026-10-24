package com.ensambla.backend.domain.service.plan;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.domain.repository.plan.PlanRepository;
import com.ensambla.backend.domain.repository.plan.PruebaPremiumRepository;
import com.ensambla.backend.domain.repository.plan.UsoPlanRepository;
import com.ensambla.backend.domain.repository.pago.RenovacionPremiumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConsultaPlanesService {

    private final PlanRepository planRepository;

    private final SuscripcionService suscripcionService;

    private final PruebaPremiumRepository pruebaPremiumRepository;

    private final UsoPlanRepository usoPlanRepository;

    private final RenovacionPremiumRepository renovacionPremiumRepository;

    /** Una única vista del negocio frente a activaciones, consumo y vencimientos concurrentes. */
    @Transactional
    public ConsultaPlanes consultar(UUID negocioId, OffsetDateTime ahora) {
        usoPlanRepository.bloquearNegocio(negocioId);
        Plan gratuito = obtenerPlan(CodigoPlan.GRATUITO);
        Plan premium = obtenerPlan(CodigoPlan.PREMIUM);
        Suscripcion suscripcion = suscripcionService.obtener(negocioId);
        boolean vigente = suscripcion.estaVigente(ahora);
        Plan actual = vigente ? planRepository.findById(suscripcion.getPlanId())
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado")) : gratuito;
        actual.validarLimitesConfigurados();
        PruebaPremium prueba = pruebaPremiumRepository.findByNegocioId(negocioId).orElse(null);
        boolean pruebaActiva = vigente && suscripcion.getEstado() == EstadoSuscripcion.PRUEBA;
        boolean puedeIniciar = prueba == null && !(vigente && actual.getCodigo() == CodigoPlan.PREMIUM)
                && Boolean.TRUE.equals(premium.getLimitesConfigurados())
                && !renovacionPremiumRepository.existeAbierta(negocioId);
        long proyectos = usoPlanRepository.contarProyectos(negocioId);
        long registros = usoPlanRepository.contarRegistros(negocioId);
        if (pruebaActiva) {
            if (prueba == null) {
                throw new ResourceNotFoundException("Prueba no encontrada");
            }
            return new ConsultaPlanes(List.of(gratuito, premium), actual, suscripcion, prueba, true, false,
                    proyectos, registros, prueba.paginasIa(), PruebaPremium.LIMITE_PAGINAS_IA,
                    "PRUEBA", prueba.inicioEn(), prueba.finEn());
        }
        OffsetDateTime inicioMes = ahora.withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()
                .withDayOfMonth(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime finMes = inicioMes.plusMonths(1);
        long paginas = usoPlanRepository.paginasEnPeriodo(negocioId, inicioMes.toLocalDate(), finMes.toLocalDate());
        return new ConsultaPlanes(List.of(gratuito, premium), actual, suscripcion, prueba, false, puedeIniciar,
                proyectos, registros, paginas, actual.getLimitePaginasIaMensuales(), "MENSUAL", inicioMes, finMes);
    }

    private Plan obtenerPlan(CodigoPlan codigo) {
        return planRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Plan no encontrado"));
    }
}
