package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ZonaNegocioTest {

    private LocalDate dia;
    private OffsetDateTime inicio;
    private Constructor<ZonaNegocio> constructor;
    private ZonaNegocio instancia;

    @Test
    @DisplayName("Antes de las 21:00 de Argentina el día argentino coincide con el de UTC")
    void antesDeLas21DeArgentinaElDiaArgentinoCoincideConElDeUtc() {
        cuandoSePideElDiaArgentinoDe("2026-10-31T20:59:00-03:00");

        entoncesElDiaEs(2026, 10, 31);
    }

    @Test
    @DisplayName("A las 21:03 de Argentina todavía es el mismo día aunque en UTC ya sea el siguiente")
    void alas2103DeArgentinaTodaviaEsElMismoDiaAunqueEnUtcYaSeaElSiguiente() {
        cuandoSePideElDiaArgentinoDe("2026-11-01T00:03:00Z");

        entoncesElDiaEs(2026, 10, 31);
    }

    @Test
    @DisplayName("Un instante justo antes de la medianoche argentina cae en el mes anterior")
    void unInstanteJustoAntesDeLaMedianocheArgentinaCaeEnElMesAnterior() {
        cuandoSePideElPrimerDiaDelMesDe("2026-11-01T02:59:59Z");

        entoncesElDiaEs(2026, 10, 1);
    }

    @Test
    @DisplayName("El mes empieza a medianoche de Argentina, que son las 03:00 UTC")
    void elMesEmpiezaAMedianocheDeArgentinaQueSonLas0300Utc() {
        cuandoSePideElPrimerDiaDelMesDe("2026-11-01T03:00:00Z");

        entoncesElDiaEs(2026, 11, 1);
    }

    @Test
    @DisplayName("El inicio del día es su medianoche argentina, expresada en UTC")
    void elInicioDelDiaEsSuMedianocheArgentinaExpresadaEnUtc() {
        cuandoSePideElInicioDelDia(LocalDate.of(2026, 11, 1));

        entoncesEmpiezaALas0300DeUtc();
    }

    @Test
    @DisplayName("No se instancia: sólo tiene métodos estáticos")
    void noSeInstanciaPorqueSoloTieneMetodosEstaticos() throws Exception {
        cuandoSeInstanciaPorReflexion();

        entoncesElConstructorEsPrivado();
    }

    // --- cuando ---
    private void cuandoSePideElDiaArgentinoDe(String instante) {
        dia = ZonaNegocio.dia(OffsetDateTime.parse(instante));
    }

    private void cuandoSePideElPrimerDiaDelMesDe(String instante) {
        dia = ZonaNegocio.primerDiaDelMes(OffsetDateTime.parse(instante));
    }

    private void cuandoSePideElInicioDelDia(LocalDate fecha) {
        inicio = ZonaNegocio.inicioDelDia(fecha);
    }

    private void cuandoSeInstanciaPorReflexion() throws Exception {
        constructor = ZonaNegocio.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        instancia = constructor.newInstance();
    }

    // --- entonces ---
    private void entoncesElDiaEs(int anio, int mes, int diaDelMes) {
        assertThat(dia).isEqualTo(LocalDate.of(anio, mes, diaDelMes));
    }

    private void entoncesEmpiezaALas0300DeUtc() {
        assertThat(inicio).isEqualTo(OffsetDateTime.parse("2026-11-01T03:00:00Z"));
        assertThat(inicio.getOffset().getTotalSeconds()).isZero();
    }

    private void entoncesElConstructorEsPrivado() {
        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        assertThat(instancia).isNotNull();
    }
}
