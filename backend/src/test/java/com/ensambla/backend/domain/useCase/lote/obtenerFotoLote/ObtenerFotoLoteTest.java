package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.lote.LoteSubidaService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerFotoLoteTest {

    private static final UUID FOTO_AJENA_ID = UUID.fromString("00000000-0000-4000-8000-000000000099");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class);
    private final ObtenerFotoLote obtenerFotoLote = new ObtenerFotoLote(negocioService,
            new LoteSubidaService(loteSubidaRepository, archivoCuadernoPort));

    @BeforeEach
    void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve los bytes y el tipo de la foto del lote del negocio de la sesión")
    void executeShouldReturnFotoOfNegocio() {
        givenLoteExists();
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE)).thenReturn(JPEG);

        FotoArchivoResponse result = obtenerFotoLote.execute(request(FOTO_ID));

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Una foto que no es del lote responde como inexistente sin leer el archivo")
    void executeShouldThrowWhenFotoIsNotInLote() {
        givenLoteExists();

        assertThatThrownBy(() -> obtenerFotoLote.execute(request(FOTO_AJENA_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Foto no encontrada");
        verifyNoInteractions(archivoCuadernoPort);
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente sin leer el archivo")
    void executeShouldThrowWhenLoteIsNotOfTheNegocio() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> obtenerFotoLote.execute(request(FOTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lote no encontrado");
        verifyNoInteractions(archivoCuadernoPort);
    }

    // --- arrange ---
    private void givenLoteExists() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    // --- helpers ---
    private static ObtenerFotoLoteRequest request(UUID fotoId) {
        return new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, fotoId);
    }
}
