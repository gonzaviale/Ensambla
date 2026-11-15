package com.ensambla.backend.domain.model.marca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SugeridorDeColoresTest {

    private static final int AZUL = 0x1E50B4;
    private static final int AZUL_PARECIDO = 0x1F50B5;
    private static final int NARANJA = 0xF08C14;
    private static final int BLANCO = 0xFFFFFF;
    private static final int NEGRO = 0x000000;
    private static final int GRIS = 0x808080;
    private static final int ROJO_CASI_NEGRO = 0x1E0000;

    private List<Integer> pixeles;

    private ColoresSugeridos sugeridos;

    @Test
    @DisplayName("Sugiere los dos colores más frecuentes y distintos del logo, el más frecuente primero")
    void sugerirShouldPickTheTwoMostFrequentDistinctColors() {
        givenUnLogoCon(AZUL, 50, NARANJA, 30);

        whenSeSugieren();

        thenSeSugiere("#1E50B4", "#F08C14");
    }

    @Test
    @DisplayName("Agrupa los colores parecidos y devuelve el promedio del grupo")
    void sugerirShouldGroupSimilarColors() {
        givenUnLogoCon(AZUL, 10, AZUL_PARECIDO, 10);
        givenMasPixeles(NARANJA, 5);

        whenSeSugieren();

        thenSeSugiere("#1E50B4", "#F08C14");
    }

    @Test
    @DisplayName("Descarta blancos, negros y grises")
    void sugerirShouldIgnoreWhiteBlackAndGray() {
        givenUnLogoCon(BLANCO, 100, NEGRO, 100);
        givenMasPixeles(GRIS, 100);
        givenMasPixeles(ROJO_CASI_NEGRO, 100);
        givenMasPixeles(NARANJA, 1);

        whenSeSugieren();

        thenSeSugiere("#F08C14", "#00506A");
    }

    @Test
    @DisplayName("Con un solo color elige como secundario el de Ensambla que más se distingue")
    void sugerirShouldFallBackToTheMostDistinctReserveColor() {
        givenUnLogoCon(AZUL, 10, AZUL_PARECIDO, 10);

        whenSeSugieren();

        thenSeSugiere("#1E50B4", "#EE8000");
    }

    @Test
    @DisplayName("Un logo sin colores propios devuelve los colores de Ensambla")
    void sugerirShouldReturnReserveWhenThereAreNoBrandColors() {
        givenUnLogoCon(BLANCO, 10, NEGRO, 10);

        whenSeSugieren();

        thenSeSugiere("#00506A", "#EE8000");
    }

    private void givenUnLogoCon(int primerColor, int primerasVeces, int segundoColor, int segundasVeces) {
        pixeles = new ArrayList<>();
        givenMasPixeles(primerColor, primerasVeces);
        givenMasPixeles(segundoColor, segundasVeces);
    }

    private void givenMasPixeles(int color, int veces) {
        pixeles.addAll(Collections.nCopies(veces, color));
    }

    private void whenSeSugieren() {
        sugeridos = SugeridorDeColores.sugerir(pixeles, KitDeMarca.COLORES_DE_ENSAMBLA);
    }

    private void thenSeSugiere(String principal, String secundario) {
        assertThat(sugeridos.principal().valor()).isEqualTo(principal);
        assertThat(sugeridos.secundario().valor()).isEqualTo(secundario);
    }
}
