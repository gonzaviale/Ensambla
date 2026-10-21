package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenCuadernoTest {

    @ParameterizedTest
    @CsvSource({"jpeg,image/jpeg", "png,image/png", "webp,image/webp"})
    @DisplayName("Detecta el tipo por los bytes de la imagen y no por lo que declara quien la manda")
    void deShouldDetectTheTypeFromTheBytes(String formato, String esperado) {
        byte[] contenido = bytesDe(formato);

        ImagenCuaderno imagen = ImagenCuaderno.de(contenido);

        thenImagenEs(imagen, contenido, esperado);
    }

    @Test
    @DisplayName("Rechaza una imagen que no llegó o está vacía")
    void deShouldRejectMissingOrEmptyImages() {
        assertThatThrownBy(() -> ImagenCuaderno.de(null))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La imagen está vacía");
        assertThatThrownBy(() -> ImagenCuaderno.de(new byte[0]))
                .isInstanceOf(BusinessRuleException.class).hasMessage("La imagen está vacía");
    }

    @Test
    @DisplayName("Rechaza una imagen que pesa más que una foto del cuaderno")
    void deShouldRejectImagesOverTheLimit() {
        byte[] enorme = new byte[(int) FotoCuaderno.MAX_BYTES + 1];

        assertThatThrownBy(() -> ImagenCuaderno.de(enorme))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("10 MB");
    }

    @ParameterizedTest
    @CsvSource({"pdf", "gif"})
    @DisplayName("Rechaza los formatos que la IA no lee, aunque la subida de lotes acepte alguno")
    void deShouldRejectFormatsTheReaderCannotSee(String formato) {
        byte[] contenido = bytesDe(formato);

        assertThatThrownBy(() -> ImagenCuaderno.de(contenido))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("JPG, PNG o WEBP");
    }

    @Test
    @DisplayName("Rechaza un archivo que no es una imagen")
    void deShouldRejectFilesThatAreNotImages() {
        assertThatThrownBy(() -> ImagenCuaderno.de(new byte[]{1, 2, 3}))
                .isInstanceOf(BusinessRuleException.class).hasMessageContaining("JPG, PNG o WEBP");
    }

    @Test
    @DisplayName("Se lee como la primera página con el tipo detectado")
    void comoPaginaShouldBeThePageNumberOne() {
        byte[] contenido = ImagenTestData.png();

        PaginaCuaderno pagina = ImagenCuaderno.de(contenido).comoPagina();

        assertThat(pagina.numero()).isEqualTo(1);
        assertThat(pagina.contenido()).isEqualTo(contenido);
        assertThat(pagina.tipoContenido()).isEqualTo("image/png");
    }

    // --- arrange ---
    private byte[] bytesDe(String formato) {
        return switch (formato) {
            case "jpeg" -> ImagenTestData.jpeg();
            case "png" -> ImagenTestData.png();
            case "webp" -> ImagenTestData.webp();
            case "pdf" -> ImagenTestData.pdf();
            default -> ImagenTestData.gif();
        };
    }

    // --- assert ---
    private void thenImagenEs(ImagenCuaderno imagen, byte[] contenido, String tipoContenido) {
        assertThat(imagen.contenido()).isEqualTo(contenido);
        assertThat(imagen.tipoContenido()).isEqualTo(tipoContenido);
    }
}
