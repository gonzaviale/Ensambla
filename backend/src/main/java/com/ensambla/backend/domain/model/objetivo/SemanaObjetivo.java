package com.ensambla.backend.domain.model.objetivo;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Las semanas de los objetivos van de lunes a domingo y se identifican por su lunes (en hora argentina). */
public final class SemanaObjetivo {

    private SemanaObjetivo() {
    }

    /** El lunes de la semana que contiene al día; un lunes es su propia semana. */
    public static LocalDate lunesDe(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
}
