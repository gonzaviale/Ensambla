package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivoId;
import com.ensambla.backend.domain.model.objetivo.EstadoEfecto;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.model.registro.RegistroId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Una decisión de la persona con su efecto sobre las ventas.
 *
 * @param efecto              {@code MEDIDO}, {@code MIDIENDO} (todavía no terminó la ventana) o {@code SIN_MEDICION}
 * @param deltaIngresoSemanal lo que cambió el ingreso semanal del producto; sólo cuando el efecto es {@code MEDIDO}
 */
public record DecisionObjetivoResponse(
        DecisionObjetivoId id,
        TipoDecision tipo,
        MiniObjetivoId miniObjetivoId,
        AvisoId avisoId,
        RegistroId registroId,
        String descripcion,
        OffsetDateTime tomadaEn,
        LocalDate semanaInicio,
        EstadoEfecto efecto,
        BigDecimal deltaIngresoSemanal) {
}
