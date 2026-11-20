package com.ensambla.backend.domain.model.lectura;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.support.ImagenTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ImagenCuadernoTest {

    private static final Map<String, Supplier<byte[]>> BYTES_POR_FORMATO = Map.of(
            "jpeg", ImagenTestData::jpeg, "png", ImagenTestData::png, "webp", ImagenTestData::webp,
            "pdf", ImagenTestData::pdf, "gif", ImagenTestData::gif);

    private ImagenCuaderno imagen;
    private PaginaCuaderno pagina;
    private Throwable rechazo;

    @ParameterizedTest
    @CsvSource({"jpeg,image/jpeg", "png,image/png", "webp,image/webp"})
    @DisplayName("Detecta el tipo por los bytes de la imagen y no por lo que declara quien la manda")
    void detectaElTipoPorLosBytesDeLaImagenYNoPorLoQueDeclaraQuienLaManda(String formato, String esperado) {
        byte[] contenido = bytesDe(formato);

        cuandoSeArmaLaImagen(contenido);

        entoncesLaImagenEs(contenido, esperado);
    }

    @Test
    @DisplayName("Rechaza una imagen que no llegó o está vacía")
    void rechazaUnaImagenQueNoLlegoOEstaVacia() {
        cuandoSeIntentaArmarLaImagen(null);
        entoncesSeRechazaCon("La imagen está vacía");

        cuandoSeIntentaArmarLaImagen(new byte[0]);
        entoncesSeRechazaCon("La imagen está vacía");
    }

    @Test
    @DisplayName("Rechaza una imagen que pesa más que una foto del cuaderno")
    void rechazaUnaImagenQuePesaMasQueUnaFotoDelCuaderno() {
        cuandoSeIntentaArmarLaImagen(new byte[(int) FotoCuaderno.MAX_BYTES + 1]);

        entoncesSeRechazaPorMensajeQueContiene("10 MB");
    }

    @ParameterizedTest
    @CsvSource({"pdf", "gif"})
    @DisplayName("Rechaza los formatos que la IA no lee, aunque la subida de lotes acepte alguno")
    void rechazaLosFormatosQueLaIaNoLeeAunqueLaSubidaDeLotesAceptaAlguno(String formato) {
        cuandoSeIntentaArmarLaImagen(bytesDe(formato));

        entoncesSeRechazaPorMensajeQueContiene("JPG, PNG o WEBP");
    }

    @Test
    @DisplayName("Rechaza un archivo que no es una imagen")
    void rechazaUnArchivoQueNoEsUnaImagen() {
        cuandoSeIntentaArmarLaImagen(new byte[]{1, 2, 3});

        entoncesSeRechazaPorMensajeQueContiene("JPG, PNG o WEBP");
    }

    @Test
    @DisplayName("Se lee como la primera página con el tipo detectado")
    void seLeeComoLaPrimeraPaginaConElTipoDetectado() {
        byte[] contenido = ImagenTestData.png();

        cuandoSeLeeComoPagina(contenido);

        entoncesEsLaPaginaUnoConElTipoPng(contenido);
    }

    // --- dado ---
    private byte[] bytesDe(String formato) {
        return BYTES_POR_FORMATO.get(formato).get();
    }

    // --- cuando ---
    private void cuandoSeArmaLaImagen(byte[] contenido) {
        imagen = ImagenCuaderno.de(contenido);
    }

    private void cuandoSeIntentaArmarLaImagen(byte[] contenido) {
        rechazo = catchThrowable(() -> cuandoSeArmaLaImagen(contenido));
    }

    private void cuandoSeLeeComoPagina(byte[] contenido) {
        pagina = ImagenCuaderno.de(contenido).comoPagina();
    }

    // --- entonces ---
    private void entoncesLaImagenEs(byte[] contenido, String tipoContenido) {
        assertThat(imagen.contenido()).isEqualTo(contenido);
        assertThat(imagen.tipoContenido()).isEqualTo(tipoContenido);
    }

    private void entoncesSeRechazaCon(String mensaje) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }

    private void entoncesSeRechazaPorMensajeQueContiene(String texto) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(texto);
    }

    private void entoncesEsLaPaginaUnoConElTipoPng(byte[] contenido) {
        assertThat(pagina.numero()).isEqualTo(1);
        assertThat(pagina.contenido()).isEqualTo(contenido);
        assertThat(pagina.tipoContenido()).isEqualTo("image/png");
    }
}
