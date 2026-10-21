package com.ensambla.backend.infrastructure.presentation.dto.plan;

import java.time.OffsetDateTime;

public record PruebaResponse(OffsetDateTime inicioEn, OffsetDateTime finEn,
                            int paginasIaUtilizadas, int limitePaginasIa) {
}
