package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SemanaObjetivoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);

    @Test
    @DisplayName("Un lunes es el lunes de su propia semana")
    void lunesDeShouldReturnTheSameDayWhenItIsMonday() {
        assertThat(SemanaObjetivo.lunesDe(LUNES)).isEqualTo(LUNES);
    }

    @Test
    @DisplayName("Un miércoles pertenece a la semana que empezó el lunes anterior")
    void lunesDeShouldReturnThePreviousMondayWhenItIsWednesday() {
        assertThat(SemanaObjetivo.lunesDe(LocalDate.of(2026, 11, 11))).isEqualTo(LUNES);
    }

    @Test
    @DisplayName("Un domingo cierra la semana que empezó seis días antes")
    void lunesDeShouldReturnTheMondayOfTheWeekWhenItIsSunday() {
        assertThat(SemanaObjetivo.lunesDe(LocalDate.of(2026, 11, 15))).isEqualTo(LUNES);
    }

    @Test
    @DisplayName("El domingo de la semana es seis días después del lunes")
    void domingoShouldReturnSixDaysAfterMonday() {
        assertThat(SemanaObjetivo.domingo(LUNES)).isEqualTo(LocalDate.of(2026, 11, 15));
    }

    @Test
    @DisplayName("La semana cierra el domingo a las 23:30 argentinas, que son las 2:30 del lunes en UTC")
    void cierreShouldBeSundayAtElevenThirtyArgentineTime() {
        assertThat(SemanaObjetivo.cierre(LUNES)).isEqualTo(OffsetDateTime.parse("2026-11-16T02:30:00Z"));
    }

    @Test
    @DisplayName("La semana se puede cerrar desde el instante de cierre, no antes")
    void cerrableShouldBeTrueFromTheClosingInstant() {
        assertThat(SemanaObjetivo.cerrable(LUNES, OffsetDateTime.parse("2026-11-16T02:30:00Z"))).isTrue();
        assertThat(SemanaObjetivo.cerrable(LUNES, OffsetDateTime.parse("2026-11-16T02:29:59Z"))).isFalse();
    }

    @Test
    @DisplayName("El domingo después de las 23:30 la última semana cerrable es la que termina ese día")
    void ultimaCerrableShouldBeThisWeekAfterTheSundayClose() {
        assertThat(SemanaObjetivo.ultimaCerrable(OffsetDateTime.parse("2026-11-16T02:45:00Z"))).isEqualTo(LUNES);
    }

    @Test
    @DisplayName("Durante la semana la última cerrable es la anterior")
    void ultimaCerrableShouldBeTheWeekBeforeDuringTheWeek() {
        assertThat(SemanaObjetivo.ultimaCerrable(OffsetDateTime.parse("2026-11-12T15:00:00Z")))
                .isEqualTo(LUNES.minusWeeks(1));
    }
}
