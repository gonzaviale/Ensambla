package com.ensambla.backend.domain.useCase.lote;

import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.JPEG;
import static org.assertj.core.api.Assertions.assertThat;

class FotoArchivoResponseTest {

    @Test
    @DisplayName("Conserva los bytes y el tipo de la foto")
    void deShouldKeepBytesAndType() {
        FotoArchivoResponse result = FotoArchivoResponse.de(new ArchivoFoto(JPEG, "image/jpeg"));

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }
}
