package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoObjetivo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record ObjetivoResponse(
        ObjetivoId id,
        String texto,
        TipoObjetivo tipo,
        BigDecimal meta,
        boolean armadoConSami,
        OffsetDateTime creadoEn) {
}
