package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoteSubidaServiceTest {

    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class);
    private final LoteSubidaService loteSubidaService =
            new LoteSubidaService(loteSubidaRepository, archivoCuadernoPort);

    @Test
    @DisplayName("Lee los bytes de una foto del negocio junto con su tipo")
    void leerShouldReturnBytesAndType() {
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE)).thenReturn(PNG);

        ArchivoFoto result = loteSubidaService.leer(NEGOCIO_ID, LoteTestData.foto());

        assertThat(result.contenido()).isEqualTo(PNG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Una foto registrada cuyo archivo falta responde como no disponible")
    void leerShouldThrowNotFoundWhenFileIsMissing() {
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE))
                .thenThrow(new LecturaCuadernoException("No se pudo acceder a la foto privada del lote"));

        assertThatThrownBy(() -> loteSubidaService.leer(NEGOCIO_ID, LoteTestData.foto()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("La foto ya no está disponible");
    }

    @Test
    @DisplayName("Devuelve el lote del negocio con sus fotos")
    void obtenerShouldReturnOwnedLote() {
        givenLoteExists();

        LoteSubida result = loteSubidaService.obtener(NEGOCIO_ID, LOTE_ID);

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        assertThat(result.getFotos()).hasSize(1);
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente")
    void obtenerShouldThrowWhenLoteBelongsToOtherNegocio() {
        givenLoteMissing();

        assertThatThrownBy(() -> loteSubidaService.obtener(NEGOCIO_ID, LOTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lote no encontrado");
    }

    // --- arrange ---
    private void givenLoteExists() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    private void givenLoteMissing() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- assert ---}
}
