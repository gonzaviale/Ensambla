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

    /** {@code true} si no tiene un comienzo: mira todo lo cargado. */
    public boolean miraTodo() {
        return dias == 0;
    }

    /** El primer día del período, o {@code null} si mira todo lo cargado. */
    public LocalDate desde(LocalDate hoy) {
        return miraTodo() ? null : hoy.minusDays(dias - 1L);
    }

    /** El último día del período (hoy), o {@code null} si mira todo lo cargado. */
    public LocalDate hasta(LocalDate hoy) {
        return miraTodo() ? null : hoy;
    }

    public boolean incluye(LocalDate fecha, LocalDate hoy) {
        return miraTodo() || estaEntre(fecha, desde(hoy), hoy);
    }

    private static boolean estaEntre(LocalDate fecha, LocalDate desde, LocalDate hasta) {
        return fecha != null && !fecha.isBefore(desde) && !fecha.isAfter(hasta);
    }
}
