package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ZonaTest {

    // 0,001° de latitud equivalen a ~111,2 m sobre la superficie terrestre.
    private static final BigDecimal LATITUD_CENTRO = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD_CENTRO = new BigDecimal("-58.562800");
    private static final BigDecimal LATITUD_A_111_METROS = new BigDecimal("-34.669300");

    @Test
    @DisplayName("La distancia al propio centro es cero")
    void distanciaMetrosShouldBeZeroAtCenter() {
        Zona zona = givenZona(1500);

        assertThat(zona.distanciaMetros(LATITUD_CENTRO, LONGITUD_CENTRO)).isZero();
    }

    @Test
    @DisplayName("Mide la distancia sobre la superficie terrestre")
    void distanciaMetrosShouldMeasureGreatCircleDistance() {
        Zona zona = givenZona(1500);

        assertThat(zona.distanciaMetros(LATITUD_A_111_METROS, LONGITUD_CENTRO)).isBetween(111.0, 111.4);
    }

    @Test
    @DisplayName("Contiene un punto dentro del radio")
    void contieneShouldAcceptPointInsideRadius() {
        Zona zona = givenZona(112);

        assertThat(zona.contiene(LATITUD_A_111_METROS, LONGITUD_CENTRO)).isTrue();
    }

    @Test
    @DisplayName("No contiene un punto fuera del radio")
    void contieneShouldRejectPointOutsideRadius() {
        Zona zona = givenZona(110);

        assertThat(zona.contiene(LATITUD_A_111_METROS, LONGITUD_CENTRO)).isFalse();
    }

    // --- arrange ---
    private Zona givenZona(int radioMetros) {
        return Zona.builder().nombre("San Justo - UNLaM")
                .latitud(LATITUD_CENTRO).longitud(LONGITUD_CENTRO).radioMetros(radioMetros).build();
    }
}
