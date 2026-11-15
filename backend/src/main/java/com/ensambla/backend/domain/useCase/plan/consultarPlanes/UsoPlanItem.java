package com.ensambla.backend.domain.useCase.plan.consultarPlanes;

import java.time.OffsetDateTime;

public record UsoPlanItem(long proyectos, long registros, long paginasIa, Integer limitePaginasIa,
                          String periodoIa, OffsetDateTime inicioPeriodoIa, OffsetDateTime finPeriodoIa) {
}
