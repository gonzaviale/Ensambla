package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.archivo.UrlFirmada;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.CLAVE;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static com.ensambla.backend.support.LoteTestData.URL_EXPIRA_EN;
import static com.ensambla.backend.support.LoteTestData.URL_LECTURA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class LoteSubidaServiceTest {

    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final AlmacenamientoArchivosPort almacenamientoArchivosPort = mock(AlmacenamientoArchivosPort.class);
    private final LoteSubidaService loteSubidaService =
            new LoteSubidaService(loteSubidaRepository, almacenamientoArchivosPort);

    @Test
    @DisplayName("Sube cada foto al bucket con su tipo y después guarda el lote pendiente")
    void subirShouldStoreFotosAndSaveLote() {
        givenRepositorySaves();

        LoteSubida result = loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG, PNG), AHORA);

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        verify(almacenamientoArchivosPort).guardar(startsWith("negocios/" + NEGOCIO_ID), eq(JPEG), eq("image/jpeg"));
        verify(almacenamientoArchivosPort).guardar(startsWith("negocios/" + NEGOCIO_ID), eq(PNG), eq("image/png"));
        thenSavedLoteHasFotos(2);
    }

    @Test
    @DisplayName("Si una foto es inválida no sube nada ni guarda el lote")
    void subirShouldNotStoreWhenFotoIsInvalid() {
        List<byte[]> contenidos = List.of(JPEG, new byte[0]);

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, contenidos, AHORA))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(almacenamientoArchivosPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla la subida de una foto borra las que ya había subido y no guarda el lote")
    void subirShouldDeleteStoredFotosWhenStorageFails() {
        givenStorageFailsOn("image/png");

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG, PNG), AHORA))
                .isInstanceOf(IllegalStateException.class);
        String subida = thenStoredClave("image/jpeg");
        String fallida = thenStoredClave("image/png");
        verify(almacenamientoArchivosPort).eliminar(subida);
        verify(almacenamientoArchivosPort, never()).eliminar(fallida);
        verifyNoInteractions(loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla el guardado del lote borra del bucket todas las fotos subidas")
    void subirShouldDeleteStoredFotosWhenSaveFails() {
        givenRepositoryFails();

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG), AHORA))
                .isInstanceOf(IllegalStateException.class);
        String subida = thenStoredClave("image/jpeg");
        verify(almacenamientoArchivosPort).eliminar(subida);
    }

    @Test
    @DisplayName("Devuelve el lote del negocio con una URL firmada de lectura por foto")
    void obtenerConUrlsDeLecturaShouldSignEveryFoto() {
        givenLoteExists();
        givenSignedUrl();

        LoteSubida result = loteSubidaService.obtenerConUrlsDeLectura(NEGOCIO_ID, LOTE_ID);

        assertThat(result.getFotos().getFirst().getUrlLectura()).isEqualTo(new UrlFirmada(URL_LECTURA, URL_EXPIRA_EN));
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente y no firma nada")
    void obtenerConUrlsDeLecturaShouldThrowWhenLoteBelongsToOtherNegocio() {
        givenLoteMissing();

        assertThatThrownBy(() -> loteSubidaService.obtenerConUrlsDeLectura(NEGOCIO_ID, LOTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lote no encontrado");
        verifyNoInteractions(almacenamientoArchivosPort);
    }

    // --- arrange ---
    private void givenRepositorySaves() {
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    private void givenRepositoryFails() {
        when(loteSubidaRepository.saveConFotos(any())).thenThrow(new IllegalStateException("base caída"));
    }

    private void givenStorageFailsOn(String tipoContenido) {
        doThrow(new IllegalStateException("bucket caído"))
                .when(almacenamientoArchivosPort).guardar(any(), any(), eq(tipoContenido));
    }

    private void givenLoteExists() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    private void givenLoteMissing() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    private void givenSignedUrl() {
        when(almacenamientoArchivosPort.generarUrlLectura(CLAVE))
                .thenReturn(new UrlFirmada(URL_LECTURA, URL_EXPIRA_EN));
    }

    // --- assert ---
    private String thenStoredClave(String tipoContenido) {
        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(almacenamientoArchivosPort).guardar(clave.capture(), any(), eq(tipoContenido));
        return clave.getValue();
    }

    private void thenSavedLoteHasFotos(int cantidad) {
        ArgumentCaptor<LoteSubida> lote = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(lote.capture());
        assertThat(lote.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getValue().getFotos()).extracting(FotoCuaderno::getPosicion).hasSize(cantidad);
    }
}
