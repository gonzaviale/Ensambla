package com.ensambla.backend.domain.useCase.zona.analizarZona;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizarZonaRequestTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");

    @Test
    @DisplayName("Tiene punto cuando vienen latitud y longitud")
    void tienePuntoShouldBeTrueWithBothCoordinates() {
        AnalizarZonaRequest request = new AnalizarZonaRequest(null, LATITUD, LONGITUD, USUARIO_ID);

        assertThat(request.tienePunto()).isTrue();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la latitud")
    void tienePuntoShouldBeFalseWithoutLatitude() {
        AnalizarZonaRequest request = new AnalizarZonaRequest("San Justo", null, LONGITUD, USUARIO_ID);

        assertThat(request.tienePunto()).isFalse();
    }

    @Test
    @DisplayName("No tiene punto cuando falta la longitud")
    void tienePuntoShouldBeFalseWithoutLongitude() {
        AnalizarZonaRequest request = new AnalizarZonaRequest("San Justo", LATITUD, null, USUARIO_ID);

        assertThat(request.tienePunto()).isFalse();
    }
}
