package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.common.ZonaNegocio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;

/** Las semanas de los objetivos van de lunes a domingo y se identifican por su lunes (en hora argentina). */
public final class SemanaObjetivo {

    /** A qué hora del domingo (argentino) se cierra la semana. */
    public static final LocalTime HORA_DE_CIERRE = LocalTime.of(23, 30);

    private SemanaObjetivo() {
    }

    /** El lunes de la semana que contiene al día; un lunes es su propia semana. */
    public static LocalDate lunesDe(LocalDate dia) {
        return dia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** El domingo de la semana que empieza el {@code lunes}. */
    public static LocalDate domingo(LocalDate lunes) {
        return lunes.plusDays(6);
    }

    /** El instante en que se cierra la semana, el domingo a la {@link #HORA_DE_CIERRE} argentina, en UTC. */
    public static OffsetDateTime cierre(LocalDate lunes) {
        return domingo(lunes).atTime(HORA_DE_CIERRE)
                .atZone(ZonaNegocio.ZONA)
                .toOffsetDateTime()
                .withOffsetSameInstant(ZoneOffset.UTC);
    }

    /** Si la semana que empieza el {@code lunes} ya se puede cerrar: el reloj llegó a su cierre. */
    public static boolean cerrable(LocalDate lunes, OffsetDateTime ahora) {
        return !ahora.isBefore(cierre(lunes));
    }

    /** El lunes de la última semana que ya se puede cerrar: la de hoy desde el domingo 23:30, si no la anterior. */
    public static LocalDate ultimaCerrable(OffsetDateTime ahora) {
        LocalDate lunes = lunesDe(ZonaNegocio.dia(ahora));
        return cerrable(lunes, ahora) ? lunes : lunes.minusWeeks(1);
    }
}
