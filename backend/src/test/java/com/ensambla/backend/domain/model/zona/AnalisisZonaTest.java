package com.ensambla.backend.domain.model.zona;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalisisZonaTest {

    @Test
    @DisplayName("Sin zona el análisis queda vacío")
    void sinZonaShouldBeEmpty() {
        assertThat(AnalisisZona.sinZona()).isEqualTo(new AnalisisZona("", 0, List.of()));
    }
}
