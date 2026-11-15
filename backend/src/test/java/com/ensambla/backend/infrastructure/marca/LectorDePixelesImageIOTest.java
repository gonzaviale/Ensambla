package com.ensambla.backend.infrastructure.marca;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.marca.LogoDeMarca;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LectorDePixelesImageIOTest {

    private static final int NARANJA = 0xF08C14;

    private final LectorDePixelesImageIO lector = new LectorDePixelesImageIO();

    private LogoDeMarca logo;

    private List<Integer> pixeles;

    @Test
    @DisplayName("Lee los píxeles opacos del logo como colores RGB")
    void leerShouldReturnOpaquePixels() throws IOException {
        givenUnLogoNaranjaDe(2, 2);

        whenSeLeenLosPixeles();

        assertThat(pixeles).containsExactly(NARANJA, NARANJA, NARANJA, NARANJA);
    }

    @Test
    @DisplayName("Muestrea un logo grande para no recorrer todos sus píxeles")
    void leerShouldSampleBigLogos() throws IOException {
        givenUnLogoNaranjaDe(1000, 1000);

        whenSeLeenLosPixeles();

        assertThat(pixeles).hasSizeLessThan(60_000).isNotEmpty();
    }

    @Test
    @DisplayName("Ignora los píxeles transparentes")
    void leerShouldIgnoreTransparentPixels() throws IOException {
        givenUnLogoTransparente();

        whenSeLeenLosPixeles();

        assertThat(pixeles).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un archivo que no es una imagen")
    void leerShouldRejectNonImage() {
        givenUnArchivoQueNoEsUnaImagen();

        assertThatThrownBy(this::whenSeLeenLosPixeles).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("Rechaza una imagen cortada a la mitad")
    void leerShouldRejectTruncatedImage() throws IOException {
        givenUnaImagenCortada();

        assertThatThrownBy(this::whenSeLeenLosPixeles).isInstanceOf(BusinessRuleException.class);
    }

    private void givenUnLogoNaranjaDe(int ancho, int alto) throws IOException {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
        pintarTodo(imagen, 0xFF000000 | NARANJA);
        logo = new LogoDeMarca(comoPng(imagen), "image/png");
    }

    private void givenUnLogoTransparente() throws IOException {
        BufferedImage imagen = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        pintarTodo(imagen, 0x00000000);
        logo = new LogoDeMarca(comoPng(imagen), "image/png");
    }

    private void givenUnArchivoQueNoEsUnaImagen() {
        logo = new LogoDeMarca(new byte[] {1, 2, 3}, "image/png");
    }

    private void givenUnaImagenCortada() throws IOException {
        BufferedImage imagen = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
        pintarTodo(imagen, 0xFF000000 | NARANJA);
        byte[] completa = comoPng(imagen);
        logo = new LogoDeMarca(Arrays.copyOf(completa, completa.length / 2), "image/png");
    }

    private void pintarTodo(BufferedImage imagen, int argb) {
        for (int x = 0; x < imagen.getWidth(); x++) {
            for (int y = 0; y < imagen.getHeight(); y++) {
                imagen.setRGB(x, y, argb);
            }
        }
    }

    private byte[] comoPng(BufferedImage imagen) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }

    private void whenSeLeenLosPixeles() {
        pixeles = lector.leer(logo);
    }
}
