package com.ensambla.backend.domain.model.reporte;

import java.time.LocalDate;

/** Los períodos que se pueden mirar en las métricas: hoy, los últimos 7 días o los últimos 30 días, terminando hoy. */
public enum PeriodoMetricas {
    HOY(1),
    SEMANA(7),
    MES(30);

    private final int dias;

    PeriodoMetricas(int dias) {
        this.dias = dias;
    }

    public int dias() {
        return dias;
    }

    /** El período que termina {@code hoy}, contando hoy. */
    public PeriodoResumen hasta(LocalDate hoy) {
        return new PeriodoResumen(hoy.minusDays(dias - 1L), hoy);
    }
}
