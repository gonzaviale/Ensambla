package com.ensambla.backend.infrastructure.presentation.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MaxBytesValidatorTest {

    private final MaxBytesValidator validator = new MaxBytesValidator();

    @ParameterizedTest
    @CsvSource({"72,true", "73,false"})
    @DisplayName("Aplica el límite de 72 bytes ASCII")
    void isValidShouldCheckByteLimit(int length, boolean expected) {
        String value = "a".repeat(length);

        boolean result = validator.isValid(value, null);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("Rechaza Unicode que cabe en caracteres pero excede 72 bytes")
    void isValidShouldRejectOversizedUnicode() {
        String value = "ñ".repeat(37);

        boolean result = validator.isValid(value, null);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Deja la obligatoriedad a NotBlank para una contraseña nula")
    void isValidShouldAllowNullForNotBlankValidation() {
        boolean result = validator.isValid(null, null);

        assertThat(result).isTrue();
    }
}

