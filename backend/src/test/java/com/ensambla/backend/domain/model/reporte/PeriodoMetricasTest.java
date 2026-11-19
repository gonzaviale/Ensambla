package com.ensambla.backend.domain.model.reporte;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodoMetricasTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    private PeriodoResumen periodo;

    @Test
    @DisplayName("Hoy es sólo el día de hoy")
    void hoyEsSoloElDiaDeHoy() {
        cuandoSePideElPeriodoQueTerminaHoy(PeriodoMetricas.HOY);

        entoncesElPeriodoVaDesdeHasta(HOY, HOY);
    }

    @Test
    @DisplayName("La semana son los últimos 7 días contando hoy")
    void laSemanaSonLosUltimos7DiasContandoHoy() {
        cuandoSePideElPeriodoQueTerminaHoy(PeriodoMetricas.SEMANA);

        entoncesElPeriodoEmpiezaEnYDuraDias(LocalDate.of(2026, 10, 13), 7);
    }

    @Test
    @DisplayName("El mes son los últimos 30 días contando hoy")
    void elMesSonLosUltimos30DiasContandoHoy() {
        cuandoSePideElPeriodoQueTerminaHoy(PeriodoMetricas.MES);

        entoncesElPeriodoDelMesEmpiezaEnYDura30Dias(LocalDate.of(2026, 9, 20));
    }

    // --- cuando ---
    private void cuandoSePideElPeriodoQueTerminaHoy(PeriodoMetricas metricas) {
        periodo = metricas.hasta(HOY);
    }

    // --- entonces ---
    private void entoncesElPeriodoVaDesdeHasta(LocalDate desde, LocalDate hasta) {
        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(periodo.hasta()).isEqualTo(hasta);
    }

    private void entoncesElPeriodoEmpiezaEnYDuraDias(LocalDate desde, int dias) {
        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(periodo.dias()).isEqualTo(dias);
    }

    private void entoncesElPeriodoDelMesEmpiezaEnYDura30Dias(LocalDate desde) {
        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(PeriodoMetricas.MES.dias()).isEqualTo(30);
    }
}
