package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PrecioTest {

    @Test
    @DisplayName("Escribe los pesos con punto de miles y sin decimales cuando no hacen falta")
    void formatoShouldWriteArgentinePesos() {
        assertThat(Precio.formato(new BigDecimal("3600"))).isEqualTo("$ 3.600");
        assertThat(Precio.formato(new BigDecimal("1250000"))).isEqualTo("$ 1.250.000");
        assertThat(Precio.formato(new BigDecimal("950.00"))).isEqualTo("$ 950");
    }

    @Test
    @DisplayName("Usa coma para los decimales")
    void formatoShouldUseACommaForDecimals() {
        assertThat(Precio.formato(new BigDecimal("1150.5"))).isEqualTo("$ 1.150,5");
    }
}
