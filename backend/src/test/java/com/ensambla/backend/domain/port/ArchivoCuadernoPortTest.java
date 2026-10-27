package com.ensambla.backend.domain.port;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArchivoCuadernoPortTest {

    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Lee los bytes de una foto del negocio junto con su tipo")
    void leerFotoShouldReturnBytesAndType() {
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE)).thenReturn(PNG);

        ArchivoFoto result = archivoCuadernoPort.leerFoto(NEGOCIO_ID, LoteTestData.foto());

        assertThat(result.contenido()).isEqualTo(PNG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Una foto registrada cuyo archivo falta responde como no disponible")
    void leerFotoShouldThrowNotFoundWhenFileIsMissing() {
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE))
                .thenThrow(new LecturaCuadernoException("No se pudo acceder a la foto privada del lote"));

        assertThatThrownBy(() -> archivoCuadernoPort.leerFoto(NEGOCIO_ID, LoteTestData.foto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("La foto ya no está disponible");
    }
}
