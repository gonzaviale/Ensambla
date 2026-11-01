package com.ensambla.backend.infrastructure.presentation.dto.zona;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizarZonaWebRequestTest {

    private static final BigDecimal LATITUD = new BigDecimal("-34.670300");
    private static final BigDecimal LONGITUD = new BigDecimal("-58.562800");
    private static final String MENSAJE = "Indicá un lugar o un punto del mapa";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Es válido con sólo un lugar escrito")
    void validateShouldAcceptPlaceOnly() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest("San Justo", null, null));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Es válido con sólo un punto del mapa")
    void validateShouldAcceptPointOnly() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest(null, LATITUD, LONGITUD));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un pedido sin lugar ni punto")
    void validateShouldRejectWhenNothingIsIndicated() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest(null, null, null));

        assertThat(result).extracting(ConstraintViolation::getMessage).containsExactly(MENSAJE);
    }

    @Test
    @DisplayName("Rechaza un lugar en blanco sin punto")
    void validateShouldRejectBlankPlaceWithoutPoint() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest("   ", null, null));

        assertThat(result).extracting(ConstraintViolation::getMessage).containsExactly(MENSAJE);
    }

    @Test
    @DisplayName("Rechaza un punto al que le falta la longitud")
    void validateShouldRejectHalfPoint() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest(null, LATITUD, null));

        assertThat(result).extracting(ConstraintViolation::getMessage).containsExactly(MENSAJE);
    }

    @Test
    @DisplayName("Rechaza una latitud fuera de rango")
    void validateShouldRejectLatitudeOutOfRange() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest(null, new BigDecimal("91"), LONGITUD));

        assertThat(result).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("latitud");
    }

    @Test
    @DisplayName("Rechaza una longitud fuera de rango")
    void validateShouldRejectLongitudeOutOfRange() {
        Set<ConstraintViolation<AnalizarZonaWebRequest>> result =
                validate(new AnalizarZonaWebRequest(null, LATITUD, new BigDecimal("-181")));

        assertThat(result).extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("longitud");
    }

    // --- act ---
    private Set<ConstraintViolation<AnalizarZonaWebRequest>> validate(AnalizarZonaWebRequest request) {
        return validator.validate(request);
    }
}
