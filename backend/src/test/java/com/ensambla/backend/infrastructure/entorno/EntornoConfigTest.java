package com.ensambla.backend.infrastructure.entorno;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class EntornoConfigTest {

    @Test
    @DisplayName("La fuente de datos de zona se configura en un único lugar, con URL, identificación y timeout")
    void entornoDelPuntoPortShouldBuildConfiguredAdapter() {
        var entorno = new EntornoConfig().entornoDelPuntoPort(
                "https://overpass.test", "Ensambla-test", Duration.ofSeconds(20));

        assertThat(entorno).isInstanceOf(OverpassEntornoAdapter.class);
    }
}
