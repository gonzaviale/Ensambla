package com.ensambla.backend.domain.model.ubicacion;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultaDireccionTest {

    @Test
    @DisplayName("Normaliza espacios y conserva la localidad")
    void deShouldNormalizeAddress() {
        ConsultaDireccion result = ConsultaDireccion.de("  Florencio   Varela 1903, San Justo  ");

        assertThat(result.texto()).isEqualTo("Florencio Varela 1903, San Justo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "    ", "abcd"})
    @DisplayName("Rechaza una dirección demasiado corta")
    void deShouldRejectShortAddress(String escrito) {
        assertThatThrownBy(() -> ConsultaDireccion.de(escrito)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una dirección de más de 200 caracteres")
    void deShouldRejectLongAddress() {
        assertThatThrownBy(() -> ConsultaDireccion.de("a".repeat(201))).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Acepta los largos límite")
    void deShouldAcceptBoundaryLengths() {
        assertThat(ConsultaDireccion.de("a".repeat(5)).texto()).hasSize(5);
        assertThat(ConsultaDireccion.de("a".repeat(200)).texto()).hasSize(200);
    }
}
