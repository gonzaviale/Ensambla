package com.ensambla.backend.domain.model.reposicion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Lo que cumple toda situación de stock; cada variante lo hereda y suma lo propio. */
abstract class SituacionStockContratoTest {

    protected abstract SituacionStock crear();

    @Test
    @DisplayName("Sugiere pedir al menos una unidad")
    void cantidadAPedirShouldBeAtLeastOneUnit() {
        SituacionStock situacion = crear();

        BigDecimal result = situacion.cantidadAPedir();

        thenPideAlMenosUnaUnidad(result);
    }

    @Test
    @DisplayName("Se describe con un texto para la persona")
    void descripcionShouldNotBeBlank() {
        SituacionStock situacion = crear();

        String result = situacion.descripcion();

        assertThat(result).isNotBlank();
    }

    @Test
    @DisplayName("Tiene una urgencia positiva")
    void urgenciaShouldBePositive() {
        SituacionStock situacion = crear();

        int result = situacion.urgencia();

        assertThat(result).isPositive();
    }

    // --- assert ---
    private void thenPideAlMenosUnaUnidad(BigDecimal cantidad) {
        assertThat(cantidad).isGreaterThanOrEqualTo(BigDecimal.ONE);
    }
}
