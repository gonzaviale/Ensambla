package com.ensambla.backend.domain.model.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoNormalizadoTest {

    @Test
    @DisplayName("Ignora tildes, mayúsculas y espacios de los bordes")
    void deShouldIgnoreAccentsCaseAndBorderSpaces() {
        assertThat(TextoNormalizado.de("  Juan PÉREZ ")).isEqualTo("juan perez");
    }

    @Test
    @DisplayName("Conserva los espacios y signos del medio")
    void deShouldKeepInnerCharacters() {
        assertThat(TextoNormalizado.de("Ñoquis  de papa-2")).isEqualTo("noquis  de papa-2");
    }
}
