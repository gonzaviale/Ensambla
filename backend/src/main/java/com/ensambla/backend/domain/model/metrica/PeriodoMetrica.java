package com.ensambla.backend.domain.model.metrica;

import java.time.LocalDate;

/** El período que mira una métrica, contado hasta hoy (hora argentina). */
public enum PeriodoMetrica {

    HOY(1),
    SEMANA(7),
    MES(30),
    TODO(0);

    private final int dias;

    PeriodoMetrica(int dias) {
        this.dias = dias;
    }

    /** El primer día del período, o {@code null} si mira todo lo cargado. */
    public LocalDate desde(LocalDate hoy) {
        return dias == 0 ? null : hoy.minusDays(dias - 1L);
    }

    public boolean incluye(LocalDate fecha, LocalDate hoy) {
        return dias == 0 || fecha != null && !fecha.isBefore(desde(hoy)) && !fecha.isAfter(hoy);
    }
}
