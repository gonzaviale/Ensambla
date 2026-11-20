package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class SemanaObjetivoTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 11, 9);
    private static final OffsetDateTime INSTANTE_DE_CIERRE = OffsetDateTime.parse("2026-11-16T02:30:00Z");

    private LocalDate dia;
    private OffsetDateTime instante;
    private boolean cerrable;

    @Test
    @DisplayName("Un lunes es el lunes de su propia semana")
    void unLunesEsElLunesDeSuPropiaSemana() {
        cuandoSePideElLunesDe(LUNES);

        entoncesElDiaEs(LUNES);
    }

    @Test
    @DisplayName("Un miércoles pertenece a la semana que empezó el lunes anterior")
    void unMiercolesPerteneceALaSemanaQueEmpezoElLunesAnterior() {
        cuandoSePideElLunesDe(LocalDate.of(2026, 11, 11));

        entoncesElDiaEs(LUNES);
    }

    @Test
    @DisplayName("Un domingo cierra la semana que empezó seis días antes")
    void unDomingoCierraLaSemanaQueEmpezoSeisDiasAntes() {
        cuandoSePideElLunesDe(LocalDate.of(2026, 11, 15));

        entoncesElDiaEs(LUNES);
    }

    @Test
    @DisplayName("El domingo de la semana es seis días después del lunes")
    void elDomingoDeLaSemanaEsSeisDiasDespuesDelLunes() {
        cuandoSePideElDomingoDeLaSemanaDelLunes();

        entoncesElDiaEs(LocalDate.of(2026, 11, 15));
    }

    @Test
    @DisplayName("La semana cierra el domingo a las 23:30 argentinas, que son las 2:30 del lunes en UTC")
    void laSemanaCierraElDomingoALas2330ArgentinasQueSonLas230DelLunesEnUtc() {
        cuandoSePideElCierreDeLaSemanaDelLunes();

        entoncesElInstanteEs(INSTANTE_DE_CIERRE);
    }

    @Test
    @DisplayName("La semana se puede cerrar desde el instante de cierre")
    void laSemanaSePuedeCerrarDesdeElInstanteDeCierre() {
        cuandoSeConsultaSiSePuedeCerrarEn(INSTANTE_DE_CIERRE);

        entoncesSePuedeCerrar();
    }

    @Test
    @DisplayName("La semana no se puede cerrar antes del instante de cierre")
    void laSemanaNoSePuedeCerrarAntesDelInstanteDeCierre() {
        cuandoSeConsultaSiSePuedeCerrarEn(INSTANTE_DE_CIERRE.minusSeconds(1));

        entoncesNoSePuedeCerrar();
    }

    @Test
    @DisplayName("El domingo después de las 23:30 la última semana cerrable es la que termina ese día")
    void elDomingoDespuesDeLas2330LaUltimaSemanaCerrableEsLaQueTerminaEseDia() {
        cuandoSePideLaUltimaCerrableEn(OffsetDateTime.parse("2026-11-16T02:45:00Z"));

        entoncesElDiaEs(LUNES);
    }

    @Test
    @DisplayName("Durante la semana la última cerrable es la anterior")
    void duranteLaSemanaLaUltimaCerrableEsLaAnterior() {
        cuandoSePideLaUltimaCerrableEn(OffsetDateTime.parse("2026-11-12T15:00:00Z"));

        entoncesElDiaEs(LUNES.minusWeeks(1));
    }

    // --- cuando ---

    private void cuandoSePideElLunesDe(LocalDate unDia) {
        dia = SemanaObjetivo.lunesDe(unDia);
    }

    private void cuandoSePideElDomingoDeLaSemanaDelLunes() {
        dia = SemanaObjetivo.domingo(LUNES);
    }

    private void cuandoSePideElCierreDeLaSemanaDelLunes() {
        instante = SemanaObjetivo.cierre(LUNES);
    }

    private void cuandoSeConsultaSiSePuedeCerrarEn(OffsetDateTime momento) {
        cerrable = SemanaObjetivo.cerrable(LUNES, momento);
    }

    private void cuandoSePideLaUltimaCerrableEn(OffsetDateTime momento) {
        dia = SemanaObjetivo.ultimaCerrable(momento);
    }

    // --- entonces ---

    private void entoncesElDiaEs(LocalDate esperado) {
        assertThat(dia).isEqualTo(esperado);
    }

    private void entoncesElInstanteEs(OffsetDateTime esperado) {
        assertThat(instante).isEqualTo(esperado);
    }

    private void entoncesSePuedeCerrar() {
        assertThat(cerrable).isTrue();
    }

    private void entoncesNoSePuedeCerrar() {
        assertThat(cerrable).isFalse();
    }
}
