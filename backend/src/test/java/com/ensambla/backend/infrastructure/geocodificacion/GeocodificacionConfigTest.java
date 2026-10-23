package com.ensambla.backend.infrastructure.geocodificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodificacionConfigTest {

    @Test
    @DisplayName("El proveedor se configura en un único lugar, con URL, identificación y timeout")
    void geocodificadorPortShouldBuildConfiguredAdapter() {
        var geocodificador = new GeocodificacionConfig().geocodificadorPort(
                "https://geocodificador.test", "Ensambla-test", Duration.ofSeconds(5));

        assertThat(geocodificador).isInstanceOf(NominatimGeocodificadorAdapter.class);
    }
}
