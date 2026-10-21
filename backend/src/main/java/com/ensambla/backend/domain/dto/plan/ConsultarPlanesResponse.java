package com.ensambla.backend.domain.dto.plan;

import java.time.OffsetDateTime;
import java.util.List;

public record ConsultarPlanesResponse(OffsetDateTime consultadoEn, List<PlanItem> planes,
                                      SuscripcionPlanItem suscripcion, UsoPlanItem uso,
                                      PruebaPremiumResponse prueba) {
}
