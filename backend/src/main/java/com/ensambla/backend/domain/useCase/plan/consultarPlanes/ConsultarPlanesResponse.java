package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;

import java.time.OffsetDateTime;
import java.util.List;

public record ConsultarPlanesResponse(OffsetDateTime consultadoEn, List<PlanItem> planes,
                                      SuscripcionPlanItem suscripcion, UsoPlanItem uso,
                                      PruebaPremiumResponse prueba) {

    public static ConsultarPlanesResponse de(ConsultaPlanes consulta, OffsetDateTime ahora) {
        PruebaPremium prueba = consulta.prueba();
        return new ConsultarPlanesResponse(ahora, consulta.planes().stream().map(PlanItem::de).toList(),
                new SuscripcionPlanItem(consulta.planActual().getCodigo().name(), consulta.pruebaActiva(),
                        consulta.puedeIniciarPrueba(), consulta.suscripcion().getInicioEn(),
                        consulta.suscripcion().getFinEn()),
                new UsoPlanItem(consulta.proyectos(), consulta.registros(), consulta.paginasIa(),
                        consulta.limitePaginasIa(), consulta.periodoIa(), consulta.inicioPeriodoIa(),
                        consulta.finPeriodoIa()),
                prueba == null ? null : PruebaPremiumResponse.de(prueba));
    }
}
