package com.ensambla.backend.domain.model.metrica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodoMetricaTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 11);

    private static final LocalDate SIN_FECHA = null;

    @Test
    @DisplayName("El período de hoy empieza hoy")
    void elPeriodoDeHoyEmpiezaHoy() {
        entoncesEmpiezaEl(PeriodoMetrica.HOY, HOY);
    }

    @Test
    @DisplayName("La semana empieza seis días antes de hoy")
    void laSemanaEmpiezaSeisDiasAntes() {
        entoncesEmpiezaEl(PeriodoMetrica.SEMANA, LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("El mes mira los últimos treinta días")
    void elMesMiraLosUltimosTreintaDias() {
        entoncesEmpiezaEl(PeriodoMetrica.MES, LocalDate.of(2026, 10, 13));
    }

    @Test
    @DisplayName("Un período con comienzo termina hoy")
    void unPeriodoConComienzoTerminaHoy() {
        entoncesTerminaEl(PeriodoMetrica.SEMANA, HOY);
    }

    @Test
    @DisplayName("'Todo' no tiene comienzo ni fin")
    void todoNoTieneComienzoNiFin() {
        entoncesEmpiezaEl(PeriodoMetrica.TODO, SIN_FECHA);
        entoncesTerminaEl(PeriodoMetrica.TODO, SIN_FECHA);
    }

    @Test
    @DisplayName("Incluye el primer día del período")
    void incluyeElPrimerDia() {
        entoncesIncluye(PeriodoMetrica.SEMANA, LocalDate.of(2026, 11, 5));
    }

    @Test
    @DisplayName("Incluye hoy")
    void incluyeHoy() {
        entoncesIncluye(PeriodoMetrica.SEMANA, HOY);
    }

    @Test
    @DisplayName("No incluye un día anterior al comienzo")
    void noIncluyeUnDiaAnteriorAlComienzo() {
        entoncesNoIncluye(PeriodoMetrica.SEMANA, LocalDate.of(2026, 11, 4));
    }

    @Test
    @DisplayName("No incluye un día futuro")
    void noIncluyeUnDiaFuturo() {
        entoncesNoIncluye(PeriodoMetrica.SEMANA, HOY.plusDays(1));
    }

    @Test
    @DisplayName("Un período con comienzo no incluye las filas sin fecha")
    void unPeriodoConComienzoNoIncluyeLasFilasSinFecha() {
        entoncesNoIncluye(PeriodoMetrica.SEMANA, SIN_FECHA);
    }

    @Test
    @DisplayName("'Todo' incluye cualquier fecha")
    void todoIncluyeCualquierFecha() {
        entoncesIncluye(PeriodoMetrica.TODO, LocalDate.of(2020, 1, 1));
    }

    @Test
    @DisplayName("'Todo' incluye las filas sin fecha")
    void todoIncluyeLasFilasSinFecha() {
        entoncesIncluye(PeriodoMetrica.TODO, SIN_FECHA);
    }

    private static void entoncesEmpiezaEl(PeriodoMetrica periodo, LocalDate dia) {
        assertThat(periodo.desde(HOY)).isEqualTo(dia);
    }

    private static void entoncesTerminaEl(PeriodoMetrica periodo, LocalDate dia) {
        assertThat(periodo.hasta(HOY)).isEqualTo(dia);
    }

    private static void entoncesIncluye(PeriodoMetrica periodo, LocalDate dia) {
        assertThat(periodo.incluye(dia, HOY)).isTrue();
    }

    private static void entoncesNoIncluye(PeriodoMetrica periodo, LocalDate dia) {
        assertThat(periodo.incluye(dia, HOY)).isFalse();
    }
}
