package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.enums.CodigoPlan;
import com.ensambla.backend.domain.model.enums.EstadoSuscripcion;
import com.ensambla.backend.domain.model.plan.Plan;
import com.ensambla.backend.domain.model.plan.Suscripcion;
import com.ensambla.backend.infrastructure.repository.entity.PlanEntity;
import com.ensambla.backend.infrastructure.repository.entity.SuscripcionEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Valores exclusivos de pruebas: no definen el catálogo comercial. */
public final class PlanTestData {

    public static final UUID PLAN_ID = UUID.fromString("bb000000-0000-4000-8000-000000000001");
    public static final UUID NEGOCIO_ID = UUID.fromString("bb000000-0000-4000-8000-000000000002");
    public static final UUID SUSCRIPCION_ID = UUID.fromString("bb000000-0000-4000-8000-000000000003");
    public static final OffsetDateTime INSTANTE = OffsetDateTime.parse("2026-10-19T12:00:00Z");

    private PlanTestData() {
    }

    public static Plan plan() {
        return Plan.builder()
                .id(PLAN_ID)
                .codigo(CodigoPlan.GRATUITO)
                .nombre("Gratuito de prueba")
                .configurado(true)
                .precioMensual(BigDecimal.ZERO)
                .moneda("ARS")
                .limiteProyectos(3)
                .limiteRegistros(100L)
                .limitePaginasIaMensuales(10)
                .funciones(List.of("FUNCION_DE_PRUEBA"))
                .creadoEn(INSTANTE)
                .actualizadoEn(INSTANTE.plusHours(1))
                .build();
    }

    public static PlanEntity planEntity() {
        return PlanEntity.builder()
                .id(PLAN_ID)
                .codigo(CodigoPlan.GRATUITO)
                .nombre("Gratuito de prueba")
                .configurado(true)
                .precioMensual(BigDecimal.ZERO)
                .moneda("ARS")
                .limiteProyectos(3)
                .limiteRegistros(100L)
                .limitePaginasIaMensuales(10)
                .funciones(List.of("FUNCION_DE_PRUEBA"))
                .creadoEn(INSTANTE)
                .actualizadoEn(INSTANTE.plusHours(1))
                .build();
    }

    public static Suscripcion suscripcion() {
        return Suscripcion.builder()
                .id(SUSCRIPCION_ID)
                .negocioId(NEGOCIO_ID)
                .planId(PLAN_ID)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusMonths(1))
                .creadoEn(INSTANTE)
                .actualizadoEn(INSTANTE.plusHours(1))
                .build();
    }

    public static SuscripcionEntity suscripcionEntity() {
        return SuscripcionEntity.builder()
                .id(SUSCRIPCION_ID)
                .negocioId(NEGOCIO_ID)
                .planId(PLAN_ID)
                .estado(EstadoSuscripcion.ACTIVA)
                .inicioEn(INSTANTE)
                .finEn(INSTANTE.plusMonths(1))
                .creadoEn(INSTANTE)
                .actualizadoEn(INSTANTE.plusHours(1))
                .build();
    }
}
