package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import java.time.OffsetDateTime;

public record SuscripcionPlanItem(String codigoPlanActual, boolean pruebaActiva, boolean puedeIniciarPrueba,
                                  OffsetDateTime inicioEn, OffsetDateTime finEn) {
}
