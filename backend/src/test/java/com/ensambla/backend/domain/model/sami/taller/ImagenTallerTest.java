package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImagenTallerTest {

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("Acepta fotos JPG, PNG y WEBP")
    void imagenShouldAcceptSupportedTypes(String tipo) {
        ImagenTaller imagen = new ImagenTaller(tipo, new byte[]{1});

        assertThat(imagen.tipo()).isEqualTo(tipo);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"image/gif", "application/pdf", "image/heic"})
    @DisplayName("Rechaza otros tipos con un mensaje para la persona")
    void imagenShouldRejectUnsupportedTypes(String tipo) {
        assertThatThrownBy(() -> new ImagenTaller(tipo, new byte[]{1}))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza una foto sin datos")
    void imagenShouldRejectMissingOrEmptyData() {
        assertThatThrownBy(() -> new ImagenTaller("image/png", null)).hasMessage(ImagenTaller.ILEGIBLE);
        assertThatThrownBy(() -> new ImagenTaller("image/png", new byte[0])).hasMessage(ImagenTaller.ILEGIBLE);
    }

    @Test
    @DisplayName("Acepta hasta 5 MB y rechaza un byte más")
    void imagenShouldRejectMoreThanFiveMegabytes() {
        ImagenTaller justo = new ImagenTaller("image/jpeg", new byte[ImagenTaller.MAX_BYTES]);

        assertThat(justo.datos()).hasSize(5 * 1024 * 1024);
        assertThatThrownBy(() -> new ImagenTaller("image/jpeg", new byte[ImagenTaller.MAX_BYTES + 1]))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage(ImagenTaller.MUY_GRANDE);
    }

    @Test
    @DisplayName("Decodifica el base64 de la foto")
    void desdeBase64ShouldDecodeData() {
        ImagenTaller imagen = ImagenTaller.desdeBase64("image/png", "AQID");

        assertThat(imagen.datos()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Acepta la data URL que entrega el navegador")
    void desdeBase64ShouldAcceptDataUrl() {
        ImagenTaller imagen = ImagenTaller.desdeBase64("image/png", "data:image/png;base64,AQID");

        assertThat(imagen.datos()).containsExactly(1, 2, 3);
    }

    @Test
    @DisplayName("Rechaza el tipo antes de decodificar")
    void desdeBase64ShouldRejectTypeBeforeDecoding() {
        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/gif", "no es base64"))
                .hasMessage(ImagenTaller.TIPO_INVALIDO);
        assertThatThrownBy(() -> ImagenTaller.desdeBase64(null, "AQID")).hasMessage(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza datos que faltan o que no son base64")
    void desdeBase64ShouldRejectMissingOrInvalidData() {
        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/png", null)).hasMessage(ImagenTaller.ILEGIBLE);
        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/png", "%%%")).hasMessage(ImagenTaller.ILEGIBLE);
        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/png", "")).hasMessage(ImagenTaller.ILEGIBLE);
    }

    @Test
    @DisplayName("Rechaza una foto de más de 5 MB sin decodificarla si el base64 ya se pasa")
    void desdeBase64ShouldRejectTooLongBase64() {
        String justo = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES]);
        String largo = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES + 3]);

        assertThat(justo).hasSize(ImagenTaller.MAX_LARGO_BASE64);
        assertThat(ImagenTaller.desdeBase64("image/webp", justo).datos()).hasSize(ImagenTaller.MAX_BYTES);
        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/webp", largo)).hasMessage(ImagenTaller.MUY_GRANDE);
    }

    @Test
    @DisplayName("Una foto de un byte más de 5 MB se rechaza al decodificarla")
    void desdeBase64ShouldRejectOneByteMoreThanFiveMegabytes() {
        String deMas = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES + 1]);

        assertThatThrownBy(() -> ImagenTaller.desdeBase64("image/webp", deMas)).hasMessage(ImagenTaller.MUY_GRANDE);
    }
}
