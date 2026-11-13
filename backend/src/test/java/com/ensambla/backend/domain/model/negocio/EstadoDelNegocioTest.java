package com.ensambla.backend.domain.model.negocio;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoDelNegocioTest {

    @Test
    @DisplayName("Sin ventas en el período no hay con qué medirlo, aunque el margen sea negativo")
    void deShouldReturnSinDatosWhenThereAreNoSales() {
        EstadoDelNegocio result = EstadoDelNegocio.de(false, -5000);

        assertThat(result).isEqualTo(EstadoDelNegocio.SIN_DATOS);
    }

    @Test
    @DisplayName("Con ventas y margen negativo pide atención")
    void deShouldReturnAtencionWhenTheMarginIsNegative() {
        EstadoDelNegocio result = EstadoDelNegocio.de(true, -1);

        assertThat(result).isEqualTo(EstadoDelNegocio.ATENCION);
    }

    @Test
    @DisplayName("Con ventas y margen en cero o positivo viene bien")
    void deShouldReturnBienWhenTheMarginIsNotNegative() {
        EstadoDelNegocio result = EstadoDelNegocio.de(true, 0);

        assertThat(result).isEqualTo(EstadoDelNegocio.BIEN);
    }
}
