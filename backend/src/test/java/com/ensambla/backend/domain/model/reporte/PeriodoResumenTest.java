package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PeriodoResumenTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 19);

    private PeriodoResumen periodo;
    private PeriodoResumen otroPeriodo;
    private Throwable excepcion;
    private Throwable otraExcepcion;
    private boolean incluyeElPrimerDia;
    private boolean incluyeElUltimoDia;
    private boolean incluyeElDiaAnterior;
    private boolean incluyeElDiaSiguiente;
    private boolean incluyeUnDiaAusente;

    @Test
    @DisplayName("Sin fechas son los últimos 30 días, contando hoy")
    void sinFechasSonLosUltimos30DiasContandoHoy() {
        cuandoSeArmaElPeriodo(null, null);

        entoncesElPeriodoVaDesdeHastaConDias(LocalDate.of(2026, 9, 20), HOY, 30);
    }

    @Test
    @DisplayName("Sin desde son los 30 días que terminan en la fecha hasta")
    void sinDesdeSonLos30DiasQueTerminanEnLaFechaHasta() {
        cuandoSeArmaElPeriodo(null, LocalDate.of(2026, 9, 30));

        entoncesElPeriodoEmpiezaEl(LocalDate.of(2026, 9, 1));
    }

    @Test
    @DisplayName("Con las dos fechas usa las que pidió la persona")
    void conLasDosFechasUsaLasQuePidioLaPersona() {
        cuandoSeArmaElPeriodo(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));

        entoncesElPeriodoEsElPedidoDeSieteDias();
    }

    @Test
    @DisplayName("Rechaza las fechas al revés")
    void rechazaLasFechasAlReves() {
        cuandoSeIntentaArmarElPeriodo(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 7));

        entoncesSeRechazaConElMensaje("La fecha desde no puede ser posterior a la fecha hasta");
    }

    @Test
    @DisplayName("Acepta un año completo y rechaza más de 366 días")
    void aceptaUnAnoCompletoYRechazaMasDe366Dias() {
        cuandoSeArmanUnAnoCompletoYUnoDeMasDe366Dias();

        entoncesElAnoCompletoDura366DiasYElExcesoSeRechaza();
    }

    @Test
    @DisplayName("Los últimos días cuentan hoy, y sin días son 30")
    void losUltimosDiasCuentanHoyYSinDiasSon30() {
        cuandoSePidenLosUltimosSieteDiasYLosUltimosDiasSinCantidad();

        entoncesLosUltimosSieteDiasCuentanHoyYSinCantidadSon30();
    }

    @Test
    @DisplayName("Los últimos días tienen que ser positivos y no más de 366")
    void losUltimosDiasTienenQueSerPositivosYNoMasDe366() {
        cuandoSePidenLosUltimosCeroDiasYLosUltimos367();

        entoncesSeRechazanPorNoSerPositivosNiMasDe366();
    }

    @Test
    @DisplayName("Incluye el primer y el último día, y no un día vacío")
    void incluyeElPrimerYElUltimoDiaYNoUnDiaVacio() {
        dadoUnPeriodoDelPrimeroAlSieteDeOctubre();

        cuandoSePreguntaPorLosDiasDelBorde();

        entoncesIncluyeLosExtremosYNoLosDiasDeAfueraNiUnoAusente();
    }

    @Test
    @DisplayName("El período anterior dura lo mismo y termina el día antes del comienzo")
    void elPeriodoAnteriorDuraLoMismoYTerminaElDiaAntesDelComienzo() {
        dadoUnPeriodoDelPrimeroAlSieteDeOctubre();

        cuandoSePideElPeriodoAnterior();

        entoncesElAnteriorVaDelVeinticuatroAlTreintaDeSeptiembreYDuraSieteDias();
    }

    // --- dado ---
    private void dadoUnPeriodoDelPrimeroAlSieteDeOctubre() {
        periodo = new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));
    }

    // --- cuando ---
    private void cuandoSeArmaElPeriodo(LocalDate desde, LocalDate hasta) {
        periodo = PeriodoResumen.armar(desde, hasta, HOY);
    }

    private void cuandoSeIntentaArmarElPeriodo(LocalDate desde, LocalDate hasta) {
        excepcion = catchThrowable(() -> PeriodoResumen.armar(desde, hasta, HOY));
    }

    private void cuandoSeArmanUnAnoCompletoYUnoDeMasDe366Dias() {
        periodo = PeriodoResumen.armar(HOY.minusDays(365), HOY, HOY);
        excepcion = catchThrowable(() -> PeriodoResumen.armar(HOY.minusDays(366), HOY, HOY));
    }

    private void cuandoSePidenLosUltimosSieteDiasYLosUltimosDiasSinCantidad() {
        periodo = PeriodoResumen.ultimosDias(7, HOY);
        otroPeriodo = PeriodoResumen.ultimosDias(null, HOY);
    }

    private void cuandoSePidenLosUltimosCeroDiasYLosUltimos367() {
        excepcion = catchThrowable(() -> PeriodoResumen.ultimosDias(0, HOY));
        otraExcepcion = catchThrowable(() -> PeriodoResumen.ultimosDias(367, HOY));
    }

    private void cuandoSePreguntaPorLosDiasDelBorde() {
        incluyeElPrimerDia = periodo.incluye(LocalDate.of(2026, 10, 1));
        incluyeElUltimoDia = periodo.incluye(LocalDate.of(2026, 10, 7));
        incluyeElDiaAnterior = periodo.incluye(LocalDate.of(2026, 9, 30));
        incluyeElDiaSiguiente = periodo.incluye(LocalDate.of(2026, 10, 8));
        incluyeUnDiaAusente = periodo.incluye(null);
    }

    private void cuandoSePideElPeriodoAnterior() {
        otroPeriodo = periodo.anterior();
    }

    // --- entonces ---
    private void entoncesElPeriodoVaDesdeHastaConDias(LocalDate desde, LocalDate hasta, int dias) {
        assertThat(periodo.hasta()).isEqualTo(hasta);
        assertThat(periodo.desde()).isEqualTo(desde);
        assertThat(periodo.dias()).isEqualTo(dias);
    }

    private void entoncesElPeriodoEmpiezaEl(LocalDate desde) {
        assertThat(periodo.desde()).isEqualTo(desde);
    }

    private void entoncesElPeriodoEsElPedidoDeSieteDias() {
        assertThat(periodo).isEqualTo(new PeriodoResumen(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7)));
        assertThat(periodo.dias()).isEqualTo(7);
    }

    private void entoncesSeRechazaConElMensaje(String mensaje) {
        assertThat(excepcion).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesElAnoCompletoDura366DiasYElExcesoSeRechaza() {
        assertThat(periodo.dias()).isEqualTo(366);
        assertThat(excepcion).isInstanceOf(BusinessRuleException.class)
                .hasMessage("El resumen no puede cubrir más de 366 días");
    }

    private void entoncesLosUltimosSieteDiasCuentanHoyYSinCantidadSon30() {
        assertThat(periodo).isEqualTo(new PeriodoResumen(HOY.minusDays(6), HOY));
        assertThat(otroPeriodo).isEqualTo(new PeriodoResumen(HOY.minusDays(29), HOY));
    }

    private void entoncesSeRechazanPorNoSerPositivosNiMasDe366() {
        assertThat(excepcion).isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cantidad de días tiene que ser mayor a cero");
        assertThat(otraExcepcion).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesIncluyeLosExtremosYNoLosDiasDeAfueraNiUnoAusente() {
        assertThat(incluyeElPrimerDia).isTrue();
        assertThat(incluyeElUltimoDia).isTrue();
        assertThat(incluyeElDiaAnterior).isFalse();
        assertThat(incluyeElDiaSiguiente).isFalse();
        assertThat(incluyeUnDiaAusente).isFalse();
    }

    private void entoncesElAnteriorVaDelVeinticuatroAlTreintaDeSeptiembreYDuraSieteDias() {
        assertThat(otroPeriodo).isEqualTo(new PeriodoResumen(LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 30)));
        assertThat(otroPeriodo.dias()).isEqualTo(7);
    }
}
