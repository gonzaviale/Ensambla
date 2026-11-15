package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.plan.CodigoPlan;
import com.ensambla.backend.domain.model.plan.ConsultaPlanes;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.PlanId;
import com.ensambla.backend.domain.model.plan.PruebaPremium;
import com.ensambla.backend.domain.model.plan.Suscripcion;

import java.time.OffsetDateTime;
import java.util.List;

public final class ConsultaPlanesTestData {

    public static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-11-01T02:30:00Z");

    public static final PlanId PREMIUM_ID = PlanId.de("bb000000-0000-4000-8000-000000000004");

    private ConsultaPlanesTestData() {
    }

    public static Plan premium() {
        Plan premium = PlanTestData.plan();
        premium.setId(PREMIUM_ID);
        premium.setCodigo(CodigoPlan.PREMIUM);
        premium.setNombre("Premium");
        premium.setConfigurado(false);
        premium.setPrecioMensual(null);
        premium.setMoneda(null);
        premium.setLimiteProyectos(null);
        premium.setLimiteRegistros(null);
        premium.setLimitePaginasIaMensuales(100);
        premium.setFunciones(List.of("RECETAS_COSTOS", "REPORTES_FILTRADOS", "EXPORTAR_EXCEL"));
        return premium;
    }

    public static PruebaPremium prueba() {
        return new PruebaPremium(PlanTestData.NEGOCIO_ID, AHORA.minusDays(3), AHORA.plusDays(4), 8);
    }

    public static ConsultaPlanes consulta(boolean conPrueba) {
        Suscripcion suscripcion = PlanTestData.suscripcion();
        return new ConsultaPlanes(List.of(PlanTestData.plan(), premium()), PlanTestData.plan(), suscripcion,
                conPrueba ? prueba() : null, false, !conPrueba, 2, 89, 6, 10, "MENSUAL",
                AHORA.withDayOfMonth(1).withHour(0).withMinute(0),
                AHORA.withDayOfMonth(1).withHour(0).withMinute(0).plusMonths(1));
    }
}
