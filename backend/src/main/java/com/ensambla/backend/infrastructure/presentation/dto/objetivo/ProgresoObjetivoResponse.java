package com.ensambla.backend.infrastructure.presentation.dto.objetivo;

import com.ensambla.backend.domain.model.objetivo.HitoObjetivo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Cómo viene un objetivo y sus mini-objetivos en curso o logrados, semana por semana.
 *
 * @param hito {@code OBJETIVO_ALCANZADO} si el resultado ya llegó a la meta; vacío si no
 */
public record ProgresoObjetivoResponse(
        BigDecimal meta,
        HitoObjetivo hito,
        List<PuntoSemanalResponse> semanas,
        List<MiniProgresoResponse> minis) {

    /** El último valor de la semana que empieza el {@code semana}; {@code estimado} si no lo midió el monitoreo. */
    public record PuntoSemanalResponse(LocalDate semana, BigDecimal valor, boolean estimado) {
    }

    /** @param hito {@code MINI_LOGRADO} o {@code SIN_AVANCE}; vacío si no hay nada para destacar */
    public record MiniProgresoResponse(
            MiniObjetivoResponse mini, HitoObjetivo hito, List<PuntoSemanalResponse> semanas) {
    }
}
