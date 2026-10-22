package com.ensambla.backend.domain.dto.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizarZonaRequestTest {

    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");

    @Test
    @DisplayName("Tiene punto cuando vienen latitud y longitud")
    void tienePuntoShouldBeTrueWithBothCoordinates() {
        AnalizarZonaRequest request = new AnalizarZonaRequest(null, LATITUD, LONGITUD);

        assertThat(request.tienePunto()).isTrue();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la latitud")
    void tienePuntoShouldBeFalseWithoutLatitude() {
        AnalizarZonaRequest request = new AnalizarZonaRequest("San Justo", null, LONGITUD);

        assertThat(request.tienePunto()).isFalse();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la longitud")
    void tienePuntoShouldBeFalseWithoutLongitude() {
        AnalizarZonaRequest request = new AnalizarZonaRequest("San Justo", LATITUD, null);

        assertThat(request.tienePunto()).isFalse();
    }
}
