package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.ColorHex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.CartelesDePrueba.kitCon;
import static com.ensambla.backend.support.CartelesDePrueba.listaDe;
import static com.ensambla.backend.support.CartelesDePrueba.promoDeAceite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpcionesDeCartelTest {

    private List<ComposicionDeCartel> opciones;

    @Test
    @DisplayName("Arma tres opciones con variantes distintas y la misma marca y los mismos textos")
    void armarShouldOfferThreeDistinctVariants() {
        whenSeArmanLasOpcionesDeLaPromo();

        assertThat(opciones).extracting(ComposicionDeCartel::variante)
                .containsExactly(VarianteDeComposicion.BLOQUE, VarianteDeComposicion.FRANJA, VarianteDeComposicion.MARCO);
        assertThat(opciones).extracting(ComposicionDeCartel::tipografia).containsOnly(opciones.getFirst().tipografia());
    }

    @Test
    @DisplayName("Al menos una opción intercambia qué color es fondo y cuál acento")
    void armarShouldSwapColorsInOneOption() {
        whenSeArmanLasOpcionesDeLaPromo();

        ColorHex fondoDelBloque = opciones.get(0).fondo();
        ColorHex fondoDeLaFranja = opciones.get(1).fondo();
        assertThat(fondoDelBloque).isEqualTo(PaletaDeCartel.PAPEL);
        assertThat(fondoDeLaFranja).isEqualTo(PaletaDeCartel.PAPEL);
        assertThat(colorDelPrimerRectangulo(opciones.get(0))).isNotEqualTo(colorDelPrimerRectangulo(opciones.get(1)));
    }

    @Test
    @DisplayName("Rechaza más líneas de las que admite el tipo de cartel")
    void armarShouldRejectTooManyLinesForTheType() {
        assertThatThrownBy(() -> OpcionesDeCartel.armar(
                kitCon("#00506A", "#EE8000"), TipoDeCartel.PROMO_PRECIO, FormatoDeCartel.A4, listaDe(3)))
                .isInstanceOf(BusinessRuleException.class);
    }

    private void whenSeArmanLasOpcionesDeLaPromo() {
        opciones = OpcionesDeCartel.armar(
                kitCon("#00506A", "#EE8000"), TipoDeCartel.PROMO_PRECIO, FormatoDeCartel.A4, promoDeAceite());
    }

    private ColorHex colorDelPrimerRectangulo(ComposicionDeCartel composicion) {
        return composicion.elementos().stream()
                .filter(Rectangulo.class::isInstance)
                .map(Rectangulo.class::cast)
                .findFirst().orElseThrow().color();
    }
}
