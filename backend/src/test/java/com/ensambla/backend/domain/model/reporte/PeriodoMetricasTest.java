package com.ensambla.backend.domain.model.reporte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodoMetricasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Test
    @DisplayName("Hoy es sólo el día de hoy")
    void hastaShouldReturnOnlyTodayForHoy() {
        PeriodoResumen periodo = PeriodoMetricas.HOY.hasta(HOY);

        assertThat(periodo.desde()).isEqualTo(HOY);
        assertThat(periodo.hasta()).isEqualTo(HOY);
    }

    @Test
    @DisplayName("La semana son los últimos 7 días contando hoy")
    void hastaShouldReturnTheLastSevenDaysForSemana() {
        PeriodoResumen periodo = PeriodoMetricas.SEMANA.hasta(HOY);

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(periodo.dias()).isEqualTo(7);
    }

    @Test
    @DisplayName("El mes son los últimos 30 días contando hoy")
    void hastaShouldReturnTheLastThirtyDaysForMes() {
        PeriodoResumen periodo = PeriodoMetricas.MES.hasta(HOY);

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(PeriodoMetricas.MES.dias()).isEqualTo(30);
    }
}
