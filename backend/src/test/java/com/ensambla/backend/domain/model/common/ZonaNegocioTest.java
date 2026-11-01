package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ZonaNegocioTest {

    @Test
    @DisplayName("Antes de las 21:00 de Argentina el día argentino coincide con el de UTC")
    void diaShouldMatchUtcBeforeArgentinaEvening() {
        OffsetDateTime instante = OffsetDateTime.parse("2026-10-31T20:59:00-03:00");

        assertThat(ZonaNegocio.dia(instante)).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    @DisplayName("A las 21:03 de Argentina todavía es el mismo día aunque en UTC ya sea el siguiente")
    void diaShouldStayOnArgentinaDayWhenUtcAlreadyChanged() {
        OffsetDateTime instante = OffsetDateTime.parse("2026-11-01T00:03:00Z");

        assertThat(ZonaNegocio.dia(instante)).isEqualTo(LocalDate.of(2026, 10, 31));
    }

    @Test
    @DisplayName("El mes empieza a medianoche de Argentina, que son las 03:00 UTC")
    void primerDiaDelMesShouldCutAtArgentinaMidnight() {
        assertThat(ZonaNegocio.primerDiaDelMes(OffsetDateTime.parse("2026-11-01T02:59:59Z")))
                .isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(ZonaNegocio.primerDiaDelMes(OffsetDateTime.parse("2026-11-01T03:00:00Z")))
                .isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    @DisplayName("El inicio del día es su medianoche argentina, expresada en UTC")
    void inicioDelDiaShouldBeArgentinaMidnightInUtc() {
        OffsetDateTime inicio = ZonaNegocio.inicioDelDia(LocalDate.of(2026, 11, 1));

        assertThat(inicio).isEqualTo(OffsetDateTime.parse("2026-11-01T03:00:00Z"));
        assertThat(inicio.getOffset().getTotalSeconds()).isZero();
    }

    @Test
    @DisplayName("No se instancia: sólo tiene métodos estáticos")
    void shouldNotBeInstantiable() throws Exception {
        Constructor<ZonaNegocio> constructor = ZonaNegocio.class.getDeclaredConstructor();

        assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        constructor.setAccessible(true);
        assertThat(constructor.newInstance()).isNotNull();
    }
}
