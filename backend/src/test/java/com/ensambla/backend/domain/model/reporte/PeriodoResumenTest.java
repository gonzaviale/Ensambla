package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PeriodoResumenTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    @Test
    @DisplayName("Sin fechas son los últimos 30 días, contando hoy")
    void armarShouldDefaultToTheLastThirtyDays() {
        PeriodoResumen periodo = PeriodoResumen.armar(null, null, HOY);

        assertThat(periodo.hasta()).isEqualTo(HOY);
        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(periodo.dias()).isEqualTo(30);
    }

    @Test
    @DisplayName("Sin desde son los 30 días que terminan en la fecha hasta")
    void armarShouldCountThirtyDaysBackFromHasta() {
        PeriodoResumen periodo = PeriodoResumen.armar(null, LocalDate.of(2026, 9, 30), HOY);

        assertThat(periodo.desde()).isEqualTo(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Con las dos fechas usa las que pidió la persona")
    void armarShouldKeepRequestedDates() {
        PeriodoResumen periodo = PeriodoResumen.armar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), HOY);

        assertThat(periodo).isEqualTo(new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)));
        assertThat(periodo.dias()).isEqualTo(7);
    }

    @Test
    @DisplayName("Rechaza las fechas al revés")
    void armarShouldRejectDesdeAfterHasta() {
        assertThatThrownBy(() -> PeriodoResumen.armar(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 7), HOY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha desde no puede ser posterior a la fecha hasta");
    }

    @Test
    @DisplayName("Acepta un año completo y rechaza más de 366 días")
    void armarShouldLimitThePeriodToOneYear() {
        assertThat(PeriodoResumen.armar(HOY.minusDays(365), HOY, HOY).dias()).isEqualTo(366);
        assertThatThrownBy(() -> PeriodoResumen.armar(HOY.minusDays(366), HOY, HOY))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El resumen no puede cubrir más de 366 días");
    }

    @Test
    @DisplayName("Incluye el primer y el último día, y no un día vacío")
    void incluyeShouldCoverBothEnds() {
        PeriodoResumen periodo = new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));

        assertThat(periodo.incluye(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(periodo.incluye(LocalDate.of(2026, 10, 7))).isTrue();
        assertThat(periodo.incluye(LocalDate.of(2026, 9, 30))).isFalse();
        assertThat(periodo.incluye(LocalDate.of(2026, 10, 8))).isFalse();
        assertThat(periodo.incluye(null)).isFalse();
    }

    @Test
    @DisplayName("El período anterior dura lo mismo y termina el día antes del comienzo")
    void anteriorShouldHaveTheSameLengthAndEndTheDayBefore() {
        PeriodoResumen anterior = new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)).anterior();

        assertThat(anterior).isEqualTo(new PeriodoResumen(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 30)));
        assertThat(anterior.dias()).isEqualTo(7);
    }
}
