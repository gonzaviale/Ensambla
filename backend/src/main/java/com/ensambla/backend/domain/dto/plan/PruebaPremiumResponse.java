package com.ensambla.backend.domain.dto.plan;

import java.time.OffsetDateTime;

public record PruebaPremiumResponse(OffsetDateTime inicioEn, OffsetDateTime finEn,
                                   int paginasIaUtilizadas, int limitePaginasIa) {
}
