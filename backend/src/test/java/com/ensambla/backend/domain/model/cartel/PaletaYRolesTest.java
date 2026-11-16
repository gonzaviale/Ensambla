package com.ensambla.backend.domain.model.cartel;

import com.ensambla.backend.domain.model.marca.ColorHex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.assertj.core.data.Offset;

import static org.assertj.core.api.Assertions.assertThat;

class PaletaYRolesTest {

    private static final ColorHex PETROLEO = ColorHex.de("#00506A");
    private static final ColorHex NARANJA = ColorHex.de("#EE8000");
    private static final ColorHex GRIS_MEDIO = ColorHex.de("#808080");
    private static final ColorHex AMARILLO_CLARO = ColorHex.de("#FFF176");
    private static final Zona ZONA = new Zona(0, 0, 1000, 1000);

    private PaletaDeCartel paleta;

    @Test
    @DisplayName("Un color principal que ya se lee con la tinta queda como está")
    void paletaShouldKeepALegibleMainColor() {
        whenSeArmaLaPaleta(PETROLEO, ColorHex.de("#8A4500"));

        assertThat(paleta.principal()).isEqualTo(PETROLEO);
        assertThat(paleta.acento()).isEqualTo(ColorHex.de("#8A4500"));
    }

    @Test
    @DisplayName("Un principal de tono medio se oscurece hasta que el blanco se lee encima")
    void paletaShouldDarkenAMidToneMainColor() {
        whenSeArmaLaPaleta(ColorHex.de("#787878"), NARANJA);

        assertThat(paleta.principal().contrasteCon(PaletaDeCartel.BLANCO)).isGreaterThanOrEqualTo(4.5);
        assertThat(paleta.principal().luminancia()).isLessThan(ColorHex.de("#787878").luminancia());
    }

    @Test
    @DisplayName("Un principal claro y apagado se aclara hasta que la pizarra se lee encima")
    void paletaShouldLightenAPaleMainColor() {
        whenSeArmaLaPaleta(GRIS_MEDIO, PETROLEO);

        assertThat(paleta.principal().contrasteCon(PaletaDeCartel.PIZARRA)).isGreaterThanOrEqualTo(4.5);
        assertThat(paleta.principal().luminancia()).isGreaterThan(GRIS_MEDIO.luminancia());
    }

    @Test
    @DisplayName("Un acento que no se lee sobre el papel se oscurece")
    void paletaShouldDarkenAnAccentThatDoesNotReadOnPaper() {
        whenSeArmaLaPaleta(PETROLEO, AMARILLO_CLARO);

        assertThat(paleta.acento().contrasteCon(PaletaDeCartel.PAPEL)).isGreaterThanOrEqualTo(4.5);
    }

    @Test
    @DisplayName("La tinta es pizarra sobre fondos claros y blanca sobre fondos oscuros")
    void tintaShouldPickTheBestForTheSurface() {
        whenSeArmaLaPaleta(PETROLEO, NARANJA);

        assertThat(paleta.tintaSobre(PaletaDeCartel.PAPEL)).isEqualTo(PaletaDeCartel.PIZARRA);
        assertThat(paleta.tintaSobre(PETROLEO)).isEqualTo(PaletaDeCartel.BLANCO);
    }

    @Test
    @DisplayName("El protagonista usa el acento sobre el papel y la tinta que se lee sobre otro color")
    void protagonistaShouldUseTheAccentOnlyOnPaper() {
        whenSeArmaLaPaleta(PETROLEO, ColorHex.de("#8A4500"));

        assertThat(RolDeTexto.PROTAGONISTA.colorSobre(paleta.papel(), paleta)).isEqualTo(paleta.acento());
        assertThat(RolDeTexto.PROTAGONISTA.colorSobre(paleta.principal(), paleta)).isEqualTo(PaletaDeCartel.BLANCO);
        assertThat(RolDeTexto.TITULAR.colorSobre(paleta.papel(), paleta)).isEqualTo(PaletaDeCartel.PIZARRA);
    }

    @Test
    @DisplayName("Los roles se reparten la zona de arriba hacia abajo sin superponerse")
    void rolesShouldShareTheZoneTopDown() {
        assertThat(RolDeTexto.TITULAR.lugarEn(ZONA).y()).isZero();
        assertThat(RolDeTexto.PROTAGONISTA.lugarEn(ZONA).y()).isEqualTo(140);
        assertThat(RolDeTexto.LINEAS.lugarEn(ZONA).y() + RolDeTexto.LINEAS.lugarEn(ZONA).alto())
                .isCloseTo(1000, Offset.offset(0.001));
    }

    @Test
    @DisplayName("Cada rol sabe su peso, su tamaño y si es el destacado")
    void roleShouldKnowItsLook() {
        assertThat(RolDeTexto.PROTAGONISTA.peso()).isEqualTo(800);
        assertThat(RolDeTexto.PROTAGONISTA.esDestacado()).isTrue();
        assertThat(RolDeTexto.BAJADA.esDestacado()).isFalse();
        assertThat(RolDeTexto.TITULAR.tamanioEn(new Zona(0, 0, 10, 100))).isCloseTo(55, Offset.offset(0.001));
    }

    private void whenSeArmaLaPaleta(ColorHex principal, ColorHex acento) {
        paleta = PaletaDeCartel.de(principal, acento);
    }
}
