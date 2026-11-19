package com.ensambla.backend.domain.model.sami.taller;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ImagenTallerTest {

    private static final byte[] UN_BYTE = {1};
    private static final byte[] SIN_DATOS = null;
    private static final String SIN_TIPO = null;
    private static final String SIN_BASE64 = null;

    private ImagenTaller imagen;
    private Throwable error;

    @ParameterizedTest
    @ValueSource(strings = {"image/jpeg", "image/png", "image/webp"})
    @DisplayName("Acepta fotos JPG, PNG y WEBP")
    void aceptaFotosJpgPngYWebp(String tipo) {
        cuandoSeAdjunta(tipo, UN_BYTE);

        entoncesLaFotoEsDeTipo(tipo);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"image/gif", "application/pdf", "image/heic"})
    @DisplayName("Rechaza otros tipos con un mensaje para la persona")
    void rechazaOtrosTipos(String tipo) {
        cuandoSeIntenta(() -> new ImagenTaller(tipo, UN_BYTE));

        entoncesSeRechazaPorque(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza una foto sin datos")
    void rechazaUnaFotoSinDatos() {
        cuandoSeIntenta(() -> new ImagenTaller("image/png", SIN_DATOS));

        entoncesSeRechazaPorque(ImagenTaller.ILEGIBLE);
    }

    @Test
    @DisplayName("Rechaza una foto vacía")
    void rechazaUnaFotoVacia() {
        cuandoSeIntenta(() -> new ImagenTaller("image/png", new byte[0]));

        entoncesSeRechazaPorque(ImagenTaller.ILEGIBLE);
    }

    @Test
    @DisplayName("Acepta una foto de 5 MB justos")
    void aceptaUnaFotoDeCincoMegasJustos() {
        cuandoSeAdjunta("image/jpeg", new byte[ImagenTaller.MAX_BYTES]);

        entoncesLaFotoPesa(5 * 1024 * 1024);
    }

    @Test
    @DisplayName("Rechaza una foto de un byte más de 5 MB")
    void rechazaUnaFotoDeUnByteMasDeCincoMegas() {
        cuandoSeIntenta(() -> new ImagenTaller("image/jpeg", new byte[ImagenTaller.MAX_BYTES + 1]));

        entoncesSeRechazaPorque(ImagenTaller.MUY_GRANDE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AQID", "data:image/png;base64,AQID"})
    @DisplayName("Decodifica el base64 de la foto, también la data URL que entrega el navegador")
    void decodificaElBase64DeLaFoto(String base64) {
        cuandoLlegaEnBase64("image/png", base64);

        entoncesLaFotoTieneLosBytes(1, 2, 3);
    }

    @Test
    @DisplayName("Rechaza el tipo antes de decodificar")
    void rechazaElTipoAntesDeDecodificar() {
        cuandoSeIntenta(() -> ImagenTaller.desdeBase64("image/gif", "no es base64"));

        entoncesSeRechazaPorque(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza una foto en base64 sin tipo")
    void rechazaUnaFotoEnBase64SinTipo() {
        cuandoSeIntenta(() -> ImagenTaller.desdeBase64(SIN_TIPO, "AQID"));

        entoncesSeRechazaPorque(ImagenTaller.TIPO_INVALIDO);
    }

    @Test
    @DisplayName("Rechaza una foto que no trae el base64")
    void rechazaUnaFotoQueNoTraeElBase64() {
        cuandoSeIntenta(() -> ImagenTaller.desdeBase64("image/png", SIN_BASE64));

        entoncesSeRechazaPorque(ImagenTaller.ILEGIBLE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"%%%", ""})
    @DisplayName("Rechaza datos que no son base64 o que están vacíos")
    void rechazaDatosQueNoSonBase64(String base64) {
        cuandoSeIntenta(() -> ImagenTaller.desdeBase64("image/png", base64));

        entoncesSeRechazaPorque(ImagenTaller.ILEGIBLE);
    }

    @Test
    @DisplayName("Un base64 del largo máximo se decodifica completo")
    void unBase64DelLargoMaximoSeDecodifica() {
        String justo = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES]);

        cuandoLlegaEnBase64("image/webp", justo);

        entoncesElBase64TieneElLargoMaximo(justo);
        entoncesLaFotoPesa(ImagenTaller.MAX_BYTES);
    }

    @Test
    @DisplayName("Rechaza una foto de más de 5 MB sin decodificarla si el base64 ya se pasa")
    void rechazaUnBase64DemasiadoLargoSinDecodificarlo() {
        String largo = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES + 3]);

        cuandoSeIntenta(() -> ImagenTaller.desdeBase64("image/webp", largo));

        entoncesSeRechazaPorque(ImagenTaller.MUY_GRANDE);
    }

    @Test
    @DisplayName("Una foto de un byte más de 5 MB se rechaza al decodificarla")
    void unaFotoDeUnByteMasSeRechazaAlDecodificarla() {
        String deMas = Base64.getEncoder().encodeToString(new byte[ImagenTaller.MAX_BYTES + 1]);

        cuandoSeIntenta(() -> ImagenTaller.desdeBase64("image/webp", deMas));

        entoncesSeRechazaPorque(ImagenTaller.MUY_GRANDE);
    }

    private void cuandoSeAdjunta(String tipo, byte[] datos) {
        imagen = new ImagenTaller(tipo, datos);
    }

    private void cuandoLlegaEnBase64(String tipo, String base64) {
        imagen = ImagenTaller.desdeBase64(tipo, base64);
    }

    private void cuandoSeIntenta(ThrowingCallable accion) {
        error = catchThrowable(accion);
    }

    private void entoncesLaFotoEsDeTipo(String tipo) {
        assertThat(imagen.tipo()).isEqualTo(tipo);
    }

    private void entoncesLaFotoPesa(int bytes) {
        assertThat(imagen.datos()).hasSize(bytes);
    }

    private void entoncesLaFotoTieneLosBytes(int... bytes) {
        assertThat(imagen.datos()).containsExactly(bytes);
    }

    private static void entoncesElBase64TieneElLargoMaximo(String base64) {
        assertThat(base64).hasSize(ImagenTaller.MAX_LARGO_BASE64);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }
}