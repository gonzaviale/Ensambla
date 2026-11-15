package com.ensambla.backend.domain.model.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ColorHexTest {

    private static final String PETROLEO_EN_MINUSCULA = " #00506a ";

    private ColorHex color;

    @Test
    @DisplayName("Un color se normaliza a mayúsculas y sin espacios")
    void deShouldNormalizeText() {
        whenSeEscribeElColor(PETROLEO_EN_MINUSCULA);

        thenElColorEs("#00506A");
    }

    @Test
    @DisplayName("Un color se arma desde su valor RGB y lo devuelve")
    void deShouldRoundTripRgb() {
        whenSeArmaElColorDesdeRgb(0x00506A);

        thenElColorEs("#00506A");
        assertThat(color.rgb()).isEqualTo(0x00506A);
        assertThat(color).hasToString("#00506A");
    }

    @Test
    @DisplayName("La distancia entre dos colores es cero con el mismo y positiva con otro")
    void distanciaAShouldMeasureHowDifferentTheyAre() {
        whenSeEscribeElColor("#000000");

        assertThat(color.distanciaA(ColorHex.de("#000000"))).isZero();
        assertThat(color.distanciaA(ColorHex.de("#030400"))).isEqualTo(5);
    }

    @Test
    @DisplayName("Rechaza un color que no es #RRGGBB")
    void deShouldRejectBadFormat() {
        assertThatThrownBy(() -> ColorHex.de("rojo")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> ColorHex.de((String) null)).isInstanceOf(BusinessRuleException.class);
    }

    private void whenSeEscribeElColor(String texto) {
        color = ColorHex.de(texto);
    }

    private void whenSeArmaElColorDesdeRgb(int rgb) {
        color = ColorHex.de(rgb);
    }

    private void thenElColorEs(String esperado) {
        assertThat(color.valor()).isEqualTo(esperado);
    }
}
