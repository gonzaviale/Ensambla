package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ObjetivoResponse(ObjetivoId id, String texto, BigDecimal meta, OffsetDateTime creadoEn) {
}
