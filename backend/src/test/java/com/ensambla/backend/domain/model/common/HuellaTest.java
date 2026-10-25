package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HuellaTest {

    @Test
    @DisplayName("La huella es el UUID derivado del texto en UTF-8")
    void deShouldBeNameUuidOfUtf8Text() {
        String result = Huella.de("Vela de soja|Ñandú");

        assertThat(result).isEqualTo(
                UUID.nameUUIDFromBytes("Vela de soja|Ñandú".getBytes(StandardCharsets.UTF_8)).toString());
    }

    @Test
    @DisplayName("El mismo texto da siempre la misma huella")
    void deShouldBeDeterministic() {
        assertThat(Huella.de("stock=3")).isEqualTo(Huella.de("stock=3"));
    }

    @Test
    @DisplayName("Textos distintos dan huellas distintas")
    void deShouldDistinguishDifferentTexts() {
        assertThat(Huella.de("stock=3")).isNotEqualTo(Huella.de("stock=4"));
    }
}
