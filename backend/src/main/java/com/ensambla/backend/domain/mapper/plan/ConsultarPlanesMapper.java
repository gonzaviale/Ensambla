package com.ensambla.backend.domain.mapper.plan;

import com.ensambla.backend.domain.dto.plan.ConsultarPlanesResponse;
import com.ensambla.backend.domain.dto.plan.PlanItem;
import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.dto.plan.SuscripcionPlanItem;
import com.ensambla.backend.domain.dto.plan.UsoPlanItem;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PruebaPremium;

import java.time.OffsetDateTime;

public class ConsultarPlanesMapper {

    public ConsultarPlanesResponse toResponse(ConsultaPlanes consulta, OffsetDateTime ahora) {
        PruebaPremium prueba = consulta.prueba();
        PruebaPremiumResponse historial = prueba == null ? null : new PruebaPremiumResponse(
                prueba.inicioEn(), prueba.finEn(), prueba.paginasIa(), PruebaPremium.LIMITE_PAGINAS_IA);
        return new ConsultarPlanesResponse(ahora, consulta.planes().stream().map(this::toItem).toList(),
                new SuscripcionPlanItem(consulta.planActual().getCodigo().name(), consulta.pruebaActiva(),
                        consulta.puedeIniciarPrueba(), consulta.suscripcion().getInicioEn(),
                        consulta.suscripcion().getFinEn()),
                new UsoPlanItem(consulta.proyectos(), consulta.registros(), consulta.paginasIa(),
                        consulta.limitePaginasIa(), consulta.periodoIa(), consulta.inicioPeriodoIa(),
                        consulta.finPeriodoIa()), historial);
    }

    private PlanItem toItem(Plan plan) {
        return new PlanItem(plan.getCodigo().name(), plan.getNombre(), Boolean.TRUE.equals(plan.getConfigurado()),
                Boolean.TRUE.equals(plan.getLimitesConfigurados()), plan.getPrecioMensual(), plan.getMoneda(),
                plan.getLimiteProyectos(), plan.getLimiteRegistros(),
                plan.getLimitePaginasIaMensuales(), plan.getFunciones());
    }
}
