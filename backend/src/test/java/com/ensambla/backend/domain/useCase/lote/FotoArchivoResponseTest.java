package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;

class FotoArchivoResponseTest {

    private FotoArchivoResponse respuesta;

    @Test
    @DisplayName("La respuesta conserva los bytes y el tipo de la foto")
    void laRespuestaConservaLosBytesYElTipoDeLaFoto() {
        cuandoSeArmaLaRespuestaDeUnaFotoJpeg();

        entoncesConservaLosBytesYElTipoDeLaFoto();
    }

    // --- cuando ---
    private void cuandoSeArmaLaRespuestaDeUnaFotoJpeg() {
        respuesta = FotoArchivoResponse.de(new ArchivoFoto(JPEG, "image/jpeg"));
    }

    // --- entonces ---
    private void entoncesConservaLosBytesYElTipoDeLaFoto() {
        assertThat(respuesta.contenido()).isEqualTo(JPEG);
        assertThat(respuesta.tipoContenido()).isEqualTo("image/jpeg");
    }
}
