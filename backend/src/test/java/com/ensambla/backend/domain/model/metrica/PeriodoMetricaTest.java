package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodoMetricaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

    @Test
    @DisplayName("Cada período empieza hoy menos sus días y 'todo' no tiene inicio")
    void desdeShouldCountBackFromToday() {
        assertThat(PeriodoMetrica.HOY.desde(HOY)).isEqualTo(HOY);
        assertThat(PeriodoMetrica.SEMANA.desde(HOY)).isEqualTo(LocalDate.of(2026, 11, 5));
        assertThat(PeriodoMetrica.MES.desde(HOY)).isEqualTo(LocalDate.of(2026, 10, 13));
        assertThat(PeriodoMetrica.TODO.desde(HOY)).isNull();
    }

    @Test
    @DisplayName("Incluye los días entre el inicio y hoy, extremos incluidos")
    void incluyeShouldAcceptDaysInsideThePeriod() {
        assertThat(PeriodoMetrica.SEMANA.incluye(LocalDate.of(2026, 11, 5), HOY)).isTrue();
        assertThat(PeriodoMetrica.SEMANA.incluye(HOY, HOY)).isTrue();
    }

    @Test
    @DisplayName("No incluye un día anterior al inicio ni uno futuro ni uno sin fecha")
    void incluyeShouldRejectDaysOutsideThePeriod() {
        assertThat(PeriodoMetrica.SEMANA.incluye(LocalDate.of(2026, 11, 4), HOY)).isFalse();
        assertThat(PeriodoMetrica.SEMANA.incluye(HOY.plusDays(1), HOY)).isFalse();
        assertThat(PeriodoMetrica.SEMANA.incluye(null, HOY)).isFalse();
    }

    @Test
    @DisplayName("'Todo' incluye cualquier fecha y también las filas sin fecha")
    void incluyeShouldAcceptEverythingWhenPeriodIsAll() {
        assertThat(PeriodoMetrica.TODO.incluye(LocalDate.of(2020, 1, 1), HOY)).isTrue();
        assertThat(PeriodoMetrica.TODO.incluye(null, HOY)).isTrue();
    }
}
