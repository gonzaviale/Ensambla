package com.ensambla.backend.domain.model.lote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static org.assertj.core.api.Assertions.assertThat;

class TipoFotoTest {

    @Test
    @DisplayName("Reconoce un JPEG por sus primeros bytes")
    void detectarShouldRecognizeJpeg() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(JPEG);

        assertThat(tipo).contains(TipoFoto.JPEG);
    }

    @Test
    @DisplayName("Reconoce un PNG por sus primeros bytes")
    void detectarShouldRecognizePng() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(PNG);

        assertThat(tipo).contains(TipoFoto.PNG);
    }

    @Test
    @DisplayName("Reconoce un WEBP por el contenedor RIFF con la marca WEBP")
    void detectarShouldRecognizeWebp() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("RIFF\0\0\0\0WEBPVP8 "));

        assertThat(tipo).contains(TipoFoto.WEBP);
    }

    @Test
    @DisplayName("Rechaza un RIFF que no es WEBP, como un audio WAV")
    void detectarShouldRejectRiffWithoutWebp() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("RIFF\0\0\0\0WAVEfmt "));

        assertThat(tipo).isEmpty();
    }

    @Test
    @DisplayName("Reconoce una foto HEIC de celular por la marca del contenedor ISO")
    void detectarShouldRecognizeHeic() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("\0\0\0\u0018ftypheic\0\0\0\0"));

        assertThat(tipo).contains(TipoFoto.HEIC);
    }

    @Test
    @DisplayName("Rechaza un contenedor ISO que no es HEIC, como un video MP4")
    void detectarShouldRejectIsoContainerWithoutHeicBrand() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("\0\0\0\u0018ftypisom\0\0\0\0"));

        assertThat(tipo).isEmpty();
    }

    @Test
    @DisplayName("Reconoce un PDF por su encabezado")
    void detectarShouldRecognizePdf() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("%PDF-1.7\n"));

        assertThat(tipo).contains(TipoFoto.PDF);
    }

    @Test
    @DisplayName("Rechaza un archivo de texto aunque el navegador diga que es una imagen")
    void detectarShouldRejectUnknownContent() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(bytes("hola, no soy una foto"));

        assertThat(tipo).isEmpty();
    }

    @Test
    @DisplayName("Rechaza un archivo demasiado corto para tener encabezado")
    void detectarShouldRejectTooShortContent() {
        Optional<TipoFoto> tipo = TipoFoto.detectar(new byte[]{1});

        assertThat(tipo).isEmpty();
    }

    @Test
    @DisplayName("Cada tipo informa el media type y la extensión con que se guarda")
    void tipoShouldExposeMediaTypeAndExtension() {
        TipoFoto tipo = TipoFoto.HEIC;

        assertThat(tipo.getMediaType()).isEqualTo("image/heic");
        assertThat(tipo.getExtension()).isEqualTo("heic");
    }

    // --- helpers ---
    private byte[] bytes(String texto) {
        return texto.getBytes(StandardCharsets.ISO_8859_1);
    }
}
