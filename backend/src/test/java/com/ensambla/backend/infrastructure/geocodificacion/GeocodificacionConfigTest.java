package com.ensambla.backend.infrastructure.geocodificacion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class GeocodificacionConfigTest {

    private Object geocodificador;

    @Test
    @DisplayName("El proveedor se configura en un único lugar, con URL, identificación y timeout")
    void elProveedorSeConfiguraEnUnUnicoLugarConUrlIdentificacionYTimeout() {
        cuandoSeConfiguraElGeocodificador();

        entoncesSeArmaElAdaptadorDeNominatim();
    }

    // --- cuando ---

    private void cuandoSeConfiguraElGeocodificador() {
        geocodificador = new GeocodificacionConfig().geocodificadorPort(
                "https://geocodificador.test", "Ensambla-test", Duration.ofSeconds(5));
    }

    // --- entonces ---

    private void entoncesSeArmaElAdaptadorDeNominatim() {
        assertThat(geocodificador).isInstanceOf(NominatimGeocodificadorAdapter.class);
    }
}
