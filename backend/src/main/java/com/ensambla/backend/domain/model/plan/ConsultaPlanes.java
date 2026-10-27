package com.ensambla.backend.domain.model.plan;

import java.time.OffsetDateTime;
import java.util.List;

public record ConsultaPlanes(List<Plan> planes, Plan planActual, Suscripcion suscripcion,
                             PruebaPremium prueba, boolean pruebaActiva, boolean puedeIniciarPrueba,
                             long proyectos, long registros, long paginasIa, Integer limitePaginasIa,
                             String periodoIa, OffsetDateTime inicioPeriodoIa, OffsetDateTime finPeriodoIa) {
}
