package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.SentidoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.TipoMiniObjetivo;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MiniObjetivoResponse(
        MiniObjetivoId id,
        int posicion,
        TipoMiniObjetivo tipo,
        SentidoMiniObjetivo sentido,
        EstadoMiniObjetivo estado,
        String titulo,
        String justificacion,
        String referencia,
        RegistroId registroId,
        BigDecimal valorInicial,
        BigDecimal valorActual,
        BigDecimal meta,
        BigDecimal avance,
        BigDecimal aporteEstimado,
        LocalDate semanaInicio,
        boolean textoConIa) {
}
