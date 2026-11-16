package com.ensambla.backend.infrastructure.cartel;

import com.ensambla.backend.domain.model.cartel.ComposicionDeCartel;
import com.ensambla.backend.domain.model.cartel.FormatoDeCartel;
import com.ensambla.backend.domain.model.cartel.SalidaDeCartel;
import com.ensambla.backend.domain.model.cartel.VarianteDeComposicion;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.ensambla.backend.support.CartelesDePrueba.kitCon;
import static com.ensambla.backend.support.CartelesDePrueba.promoDeAceite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ExportadoresDeCartelTest {

    private static final int PETROLEO = 0x00506A;

    private final ExportadorPngDeCartel png = new ExportadorPngDeCartel();
    private final ExportadorPdfDeCartel pdf = new ExportadorPdfDeCartel();

    private ComposicionDeCartel composicion;

    private byte[] archivo;

    @Test
    @DisplayName("El PNG tiene el tamaño del formato y pinta el fondo y los bloques con los colores de la marca")
    void pngShouldHaveTheFormatSizeAndTheBrandColors() throws IOException {
        givenUnCartel(VarianteDeComposicion.BLOQUE, FormatoDeCartel.CUADRADO);

        whenSeExportaEnPng();

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(archivo));
        assertThat(imagen.getWidth()).isEqualTo(1080);
        assertThat(imagen.getHeight()).isEqualTo(1080);
        assertThat(imagen.getRGB(5, 5) & 0xFFFFFF).isEqualTo(PETROLEO);
        assertThat(imagen.getRGB(5, 1075) & 0xFFFFFF).isEqualTo(0xFFFDFA);
        assertThat(png.salida()).isEqualTo(SalidaDeCartel.PNG);
    }

    @Test
    @DisplayName("El PNG de una variante centrada también se arma con su tarjeta")
    void pngShouldDrawTheCenteredVariant() throws IOException {
        givenUnCartel(VarianteDeComposicion.MARCO, FormatoDeCartel.VERTICAL);

        whenSeExportaEnPng();

        BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(archivo));
        assertThat(imagen.getHeight()).isEqualTo(1350);
        assertThat(imagen.getRGB(5, 5) & 0xFFFFFF).isEqualTo(PETROLEO);
        assertThat(imagen.getRGB(100, 100) & 0xFFFFFF).isEqualTo(0xFFFDFA);
    }

    @Test
    @DisplayName("El PDF es una página con la medida del A4 y del A5 en puntos")
    void pdfShouldHaveThePageSizeOfTheFormat() throws IOException {
        givenUnCartel(VarianteDeComposicion.FRANJA, FormatoDeCartel.A4);

        whenSeExportaEnPdf();

        assertThat(new String(archivo, 0, 5)).isEqualTo("%PDF-");
        PdfReader lector = new PdfReader(archivo);
        assertThat(lector.getNumberOfPages()).isEqualTo(1);
        assertThat(lector.getPageSize(1).getWidth()).isCloseTo(595.3f, within(0.5f));
        assertThat(lector.getPageSize(1).getHeight()).isCloseTo(841.9f, within(0.5f));
        assertThat(pdf.salida()).isEqualTo(SalidaDeCartel.PDF);
    }

    @Test
    @DisplayName("El PDF del A5 con la variante centrada mide 420 por 595 puntos")
    void pdfShouldDrawTheA5CenteredVariant() throws IOException {
        givenUnCartel(VarianteDeComposicion.MARCO, FormatoDeCartel.A5);

        whenSeExportaEnPdf();

        PdfReader lector = new PdfReader(archivo);
        assertThat(lector.getPageSize(1).getWidth()).isCloseTo(419.5f, within(0.5f));
        assertThat(lector.getPageSize(1).getHeight()).isCloseTo(595.3f, within(0.5f));
    }

    private void givenUnCartel(VarianteDeComposicion variante, FormatoDeCartel formato) {
        composicion = variante.componer(kitCon("#00506A", "#EE8000"), formato, promoDeAceite());
    }

    private void whenSeExportaEnPng() {
        archivo = png.exportar(composicion);
    }

    private void whenSeExportaEnPdf() {
        archivo = pdf.exportar(composicion);
    }
}
