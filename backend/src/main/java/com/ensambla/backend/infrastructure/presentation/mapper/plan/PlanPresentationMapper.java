package com.ensambla.backend.infrastructure.presentation.mapper.plan;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.PlanItem;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPruebaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import com.ensambla.backend.domain.useCase.plan.listarCatalogoPlanes.ListarCatalogoPlanesResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlanPresentationMapper {

    public IniciarPruebaRequest toRequest(UsuarioId usuarioId) {
        return new IniciarPruebaRequest(usuarioId);
    }

    public PruebaResponse toResponse(PruebaPremiumResponse response) {
        return new PruebaResponse(response.inicioEn(), response.finEn(),
                response.paginasIaUtilizadas(), response.limitePaginasIa());
    }

    public PlanesResponse toResponse(ConsultarPlanesResponse response) {
        var suscripcion = response.suscripcion();
        var uso = response.uso();
        return new PlanesResponse(response.consultadoEn(), response.planes().stream().map(this::toPlan).toList(),
                new PlanesResponse.SuscripcionResponse(suscripcion.codigoPlanActual(), suscripcion.pruebaActiva(),
                        suscripcion.puedeIniciarPrueba(), suscripcion.inicioEn(), suscripcion.finEn()),
                new PlanesResponse.UsoResponse(uso.proyectos(), uso.registros(), uso.paginasIa(),
                        uso.limitePaginasIa(), uso.periodoIa(), uso.inicioPeriodoIa(), uso.finPeriodoIa()),
                response.prueba() == null ? null : toResponse(response.prueba()));
    }

    public List<PlanesResponse.PlanResponse> toResponse(ListarCatalogoPlanesResponse response) {
        return response.planes().stream().map(plan -> new PlanesResponse.PlanResponse(plan.codigo(), plan.nombre(),
                plan.precioConfigurado(), plan.limitesConfigurados(), plan.precioMensual(), plan.moneda(),
                plan.limiteProyectos(), plan.limiteRegistros(), plan.limitePaginasIaMensuales(), plan.funciones()))
                .toList();
    }

    private PlanesResponse.PlanResponse toPlan(PlanItem plan) {
        return new PlanesResponse.PlanResponse(plan.codigo(), plan.nombre(), plan.precioConfigurado(),
                plan.limitesConfigurados(), plan.precioMensual(), plan.moneda(), plan.limiteProyectos(),
                plan.limiteRegistros(),
                plan.limitePaginasIaMensuales(), plan.funciones());
    }
}
