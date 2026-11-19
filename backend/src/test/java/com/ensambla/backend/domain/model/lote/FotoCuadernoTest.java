package com.ensambla.backend.domain.model.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LotesDePrueba.unArchivoDeMasDeDiezMegas;
import static com.ensambla.backend.support.LotesDePrueba.unArchivoDeTexto;
import static com.ensambla.backend.support.LotesDePrueba.unArchivoVacio;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FotoCuadernoTest {

    private static final short POSICION = 2;

    private FotoCuaderno foto;
    private FotoCuaderno otraFoto;
    private Throwable rechazo;

    @Test
    @DisplayName("La foto nueva tiene el tipo detectado, el tamaño y una clave en la carpeta privada del negocio")
    void laFotoNuevaTieneElTipoDetectadoElTamanioYUnaClaveEnLaCarpetaPrivadaDelNegocio() {
        cuandoSeArmaUnaFotoJpeg();

        entoncesLaFotoQuedaEnLaCarpetaDelNegocioConSusDatos();
    }

    @Test
    @DisplayName("Dos fotos iguales reciben claves distintas")
    void dosFotosIgualesRecibenClavesDistintas() {
        cuandoSeArmanDosFotosJpegIguales();

        entoncesLasDosFotosTienenClavesDistintas();
    }

    @Test
    @DisplayName("Un archivo vacío se rechaza indicando qué foto es")
    void unArchivoVacioSeRechazaIndicandoQueFotoEs() {
        cuandoSeIntentaArmarUnaFotoConElArchivo(unArchivoVacio());

        entoncesSeRechazaPorque("La foto 3 está vacía");
    }

    @Test
    @DisplayName("Un archivo de más de 10 MB se rechaza")
    void unArchivoDeMasDeDiezMegasSeRechaza() {
        cuandoSeIntentaArmarUnaFotoConElArchivo(unArchivoDeMasDeDiezMegas(FotoCuaderno.MAX_BYTES));

        entoncesSeRechazaPorque("La foto 3 supera el máximo de 10 MB");
    }

    @Test
    @DisplayName("Un archivo que no es una imagen ni un PDF se rechaza")
    void unArchivoQueNoEsUnaImagenNiUnPdfSeRechaza() {
        cuandoSeIntentaArmarUnaFotoConElArchivo(unArchivoDeTexto());

        entoncesSeRechazaPorque("La foto 3 no es una imagen JPEG, PNG, WEBP o HEIC ni un PDF");
    }

    // --- cuando ---
    private void cuandoSeArmaUnaFotoJpeg() {
        foto = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);
    }

    private void cuandoSeArmanDosFotosJpegIguales() {
        foto = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);
        otraFoto = FotoCuaderno.nueva(NEGOCIO_ID, JPEG, POSICION, AHORA);
    }

    private void cuandoSeIntentaArmarUnaFotoConElArchivo(byte[] contenido) {
        rechazo = catchThrowable(() -> FotoCuaderno.nueva(NEGOCIO_ID, contenido, POSICION, AHORA));
    }

    // --- entonces ---
    private void entoncesLaFotoQuedaEnLaCarpetaDelNegocioConSusDatos() {
        assertThat(foto.getClaveAlmacenamiento()).startsWith(NEGOCIO_ID + "/fotos/").endsWith(".jpg");
        assertThat(foto.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(foto.getTipoContenido()).isEqualTo("image/jpeg");
        assertThat(foto.getTamanioBytes()).isEqualTo(JPEG.length);
        assertThat(foto.getRotacion()).isZero();
        assertThat(foto.getPosicion()).isEqualTo(POSICION);
        assertThat(foto.getCreadoEn()).isEqualTo(AHORA);
        assertThat(foto.getActualizadoEn()).isEqualTo(AHORA);
        assertThat(foto.getContenido()).isEqualTo(JPEG);
    }

    private void entoncesLasDosFotosTienenClavesDistintas() {
        assertThat(otraFoto.getClaveAlmacenamiento()).isNotEqualTo(foto.getClaveAlmacenamiento());
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessage(motivo);
    }
}
