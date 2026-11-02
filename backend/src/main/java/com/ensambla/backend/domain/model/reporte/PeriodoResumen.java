package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * El período que cubre un resumen del negocio: desde y hasta, ambos días incluidos y en hora argentina.
 */
public record PeriodoResumen(LocalDate desde, LocalDate hasta) {

    /** Días del período cuando la persona no elige fechas. */
    public static final int DIAS_POR_DEFECTO = 30;

    /** Un resumen de más de un año no es un resumen: se pide por partes. */
    public static final int MAXIMO_DIAS = 366;

    /**
     * El período pedido; sin {@code hasta} es hoy y sin {@code desde} son los {@value #DIAS_POR_DEFECTO} días que
     * terminan en {@code hasta}.
     *
     * @throws BusinessRuleException si las fechas están al revés o el período supera {@value #MAXIMO_DIAS} días
     */
    public static PeriodoResumen armar(LocalDate desde, LocalDate hasta, LocalDate hoy) {
        LocalDate fin = hasta == null ? hoy : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(DIAS_POR_DEFECTO - 1L) : desde;
        if (inicio.isAfter(fin)) {
            throw new BusinessRuleException("La fecha desde no puede ser posterior a la fecha hasta");
        }
        PeriodoResumen periodo = new PeriodoResumen(inicio, fin);
        if (periodo.dias() > MAXIMO_DIAS) {
            throw new BusinessRuleException("El resumen no puede cubrir más de " + MAXIMO_DIAS + " días");
        }
        return periodo;
    }

    /** Cuántos días cubre, contando el primero y el último. */
    public long dias() {
        return ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    public boolean incluye(LocalDate dia) {
        return dia != null && !dia.isBefore(desde) && !dia.isAfter(hasta);
    }

    /** El período de la misma duración que termina el día anterior al comienzo de éste, para comparar. */
    public PeriodoResumen anterior() {
        return new PeriodoResumen(desde.minusDays(dias()), desde.minusDays(1));
    }
}
