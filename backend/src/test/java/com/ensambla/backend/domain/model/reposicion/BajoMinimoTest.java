package com.ensambla.backend.domain.model.reposicion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BajoMinimoTest extends SituacionStockContratoTest {

    private static final BigDecimal MINIMO = BigDecimal.TEN;

    @Override
    protected SituacionStock crear() {
        return bajoMinimoCon(new BigDecimal("3"));
    }

    @Test
    @DisplayName("Cuenta cuántas unidades quedan y cuál es el mínimo")
    void descripcionShouldTellHowManyAreLeft() {
        SituacionStock situacion = crear();

        String result = situacion.descripcion();

        assertThat(result).isEqualTo("Quedan 3 (mínimo 10)");
    }

    @Test
    @DisplayName("Muestra los decimales con coma")
    void descripcionShouldUseCommaForDecimals() {
        SituacionStock situacion = bajoMinimoCon(new BigDecimal("2.50"));

        String result = situacion.descripcion();

        assertThat(result).isEqualTo("Quedan 2,5 (mínimo 10)");
    }

    @Test
    @DisplayName("Pide lo que falta para llegar al doble del mínimo")
    void cantidadAPedirShouldReachDoubleTheMinimum() {
        SituacionStock situacion = bajoMinimoCon(new BigDecimal("3"));

        BigDecimal result = situacion.cantidadAPedir();

        assertThat(result).isEqualByComparingTo("17");
    }

    @Test
    @DisplayName("Llegar justo al mínimo ya es estar bajo")
    void cantidadAPedirShouldAskWhenTheStockEqualsTheMinimum() {
        SituacionStock situacion = bajoMinimoCon(MINIMO);

        BigDecimal result = situacion.cantidadAPedir();

        assertThat(result).isEqualByComparingTo("10");
    }

    // --- arrange ---
    private SituacionStock bajoMinimoCon(BigDecimal cantidad) {
        return SituacionStock.detectar(cantidad, MINIMO).orElseThrow();
    }
}
