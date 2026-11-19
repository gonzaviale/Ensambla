package com.ensambla.backend.domain.model.lote;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static com.ensambla.backend.support.LotesDePrueba.bytesDeTexto;
import static org.assertj.core.api.Assertions.assertThat;

class TipoFotoTest {

    private Optional<TipoFoto> tipo;

    @Test
    @DisplayName("Un JPEG se reconoce por sus primeros bytes")
    void unJpegSeReconocePorSusPrimerosBytes() {
        cuandoSeDetectaElTipoDe(JPEG);

        entoncesSeReconoceComo(TipoFoto.JPEG);
    }

    @Test
    @DisplayName("Un PNG se reconoce por sus primeros bytes")
    void unPngSeReconocePorSusPrimerosBytes() {
        cuandoSeDetectaElTipoDe(PNG);

        entoncesSeReconoceComo(TipoFoto.PNG);
    }

    @Test
    @DisplayName("Un WEBP se reconoce por el contenedor RIFF con la marca WEBP")
    void unWebpSeReconocePorElContenedorRiffConLaMarcaWebp() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("RIFF\0\0\0\0WEBPVP8 "));

        entoncesSeReconoceComo(TipoFoto.WEBP);
    }

    @Test
    @DisplayName("Un RIFF que no es WEBP, como un audio WAV, no se reconoce")
    void unRiffQueNoEsWebpComoUnAudioWavNoSeReconoce() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("RIFF\0\0\0\0WAVEfmt "));

        entoncesNoSeReconoceNingunTipo();
    }

    @Test
    @DisplayName("Una foto HEIC de celular se reconoce por la marca del contenedor ISO")
    void unaFotoHeicDeCelularSeReconocePorLaMarcaDelContenedorIso() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("\0\0\0\u0018ftypheic\0\0\0\0"));

        entoncesSeReconoceComo(TipoFoto.HEIC);
    }

    @Test
    @DisplayName("Un contenedor ISO que no es HEIC, como un video MP4, no se reconoce")
    void unContenedorIsoQueNoEsHeicComoUnVideoMp4NoSeReconoce() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("\0\0\0\u0018ftypisom\0\0\0\0"));

        entoncesNoSeReconoceNingunTipo();
    }

    @Test
    @DisplayName("Un PDF se reconoce por su encabezado")
    void unPdfSeReconocePorSuEncabezado() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("%PDF-1.7\n"));

        entoncesSeReconoceComo(TipoFoto.PDF);
    }

    @Test
    @DisplayName("Un archivo de texto no se reconoce aunque el navegador diga que es una imagen")
    void unArchivoDeTextoNoSeReconoceAunqueElNavegadorDigaQueEsUnaImagen() {
        cuandoSeDetectaElTipoDe(bytesDeTexto("hola, no soy una foto"));

        entoncesNoSeReconoceNingunTipo();
    }

    @Test
    @DisplayName("Un archivo demasiado corto para tener encabezado no se reconoce")
    void unArchivoDemasiadoCortoParaTenerEncabezadoNoSeReconoce() {
        cuandoSeDetectaElTipoDe(new byte[]{1});

        entoncesNoSeReconoceNingunTipo();
    }

    @Test
    @DisplayName("Cada tipo informa el media type y la extensión con que se guarda")
    void cadaTipoInformaElMediaTypeYLaExtensionConQueSeGuarda() {
        entoncesElTipoHeicSeGuardaComoImageHeicConExtensionHeic();
    }

    // --- cuando ---
    private void cuandoSeDetectaElTipoDe(byte[] contenido) {
        tipo = TipoFoto.detectar(contenido);
    }

    // --- entonces ---
    private void entoncesSeReconoceComo(TipoFoto esperado) {
        assertThat(tipo).contains(esperado);
    }

    private void entoncesNoSeReconoceNingunTipo() {
        assertThat(tipo).isEmpty();
    }

    private void entoncesElTipoHeicSeGuardaComoImageHeicConExtensionHeic() {
        assertThat(TipoFoto.HEIC.getMediaType()).isEqualTo("image/heic");
        assertThat(TipoFoto.HEIC.getExtension()).isEqualTo("heic");
    }
}
