package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

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
}
