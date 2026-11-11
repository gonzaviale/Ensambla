package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;

public record CandidatoMiniObjetivoResponse(
        TipoMiniObjetivo tipo,
        SentidoMiniObjetivo sentido,
        String titulo,
        String justificacion,
        String referencia,
        RegistroId registroId,
        BigDecimal valorInicial,
        BigDecimal meta,
        BigDecimal aporteEstimado) {
}
