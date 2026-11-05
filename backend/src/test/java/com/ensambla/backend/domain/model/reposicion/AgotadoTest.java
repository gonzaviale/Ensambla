package com.ensambla.backend.domain.model.reposicion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AgotadoTest extends SituacionStockContratoTest {

    private static final BigDecimal MINIMO = BigDecimal.TEN;

    @Override
    protected SituacionStock crear() {
        return agotadoCon(BigDecimal.ZERO, MINIMO);
    }

    @Test
    @DisplayName("Se describe como agotado")
    void descripcionShouldSayAgotado() {
        SituacionStock situacion = crear();

        String result = situacion.descripcion();

        assertThat(result).isEqualTo("Agotado");
    }

    @Test
    @DisplayName("Es más urgente que un producto que solo está bajo el mínimo")
    void urgenciaShouldBeHigherThanBajoMinimo() {
        SituacionStock agotado = crear();
        SituacionStock bajo = SituacionStock.detectar(new BigDecimal("3"), MINIMO).orElseThrow();

        assertThat(agotado.urgencia()).isGreaterThan(bajo.urgencia());
    }

    @Test
    @DisplayName("Pide hasta el doble del mínimo")
    void cantidadAPedirShouldReachDoubleTheMinimum() {
        SituacionStock situacion = agotadoCon(BigDecimal.ZERO, MINIMO);

        BigDecimal result = situacion.cantidadAPedir();

        assertThat(result).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Sin mínimo definido pide diez unidades")
    void cantidadAPedirShouldBeTenWhenThereIsNoMinimum() {
        SituacionStock situacion = agotadoCon(BigDecimal.ZERO, null);

        BigDecimal result = situacion.cantidadAPedir();

        assertThat(result).isEqualByComparingTo("10");
    }

    // --- arrange ---
    private SituacionStock agotadoCon(BigDecimal cantidad, BigDecimal minimo) {
        return SituacionStock.detectar(cantidad, minimo).orElseThrow();
    }
}
