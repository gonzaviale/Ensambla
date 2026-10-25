package com.ensambla.backend.domain.useCase.lote.subirLote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.support.LoteTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
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

class SubirLoteTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final SubirLote subirLote = new SubirLote(negocioRepository, loteSubidaRepository, archivoCuadernoPort,
            controlPlanService, new TransaccionDirecta(), bloqueRepository,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Guarda cada foto en la carpeta del negocio de la sesión y después guarda el lote pendiente")
    void executeShouldStoreFotosAndSaveLote() {
        givenNegocio();
        givenRepositorySaves();

        LoteItem result = subir(List.of(JPEG, PNG), null);

        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.estado()).isEqualTo("PENDIENTE");
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(JPEG));
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(PNG));
        verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 2, AHORA);
        thenSavedLoteHasFotos(2);
    }

    @Test
    @DisplayName("Con un bloque destino del negocio lo valida y lo guarda en el lote")
    void executeShouldSaveValidatedBloqueDestino() {
        givenNegocio();
        givenRepositorySaves();

        subir(List.of(JPEG), BLOQUE_ID);

        verify(bloqueRepository).obtener(BLOQUE_ID, NEGOCIO_ID);
        thenSavedLoteHasBloqueDestino();
    }

    @Test
    @DisplayName("Un bloque destino de otro negocio responde como inexistente y no sube nada")
    void executeShouldRejectBloqueDestinoOfOtherNegocio() {
        givenNegocio();
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("Bloque no encontrado"));

        assertThatThrownBy(() -> subir(List.of(JPEG), BLOQUE_ID)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Sin bloque destino no consulta bloques")
    void executeShouldNotLookUpBloqueWhenNoDestino() {
        givenNegocio();
        givenRepositorySaves();

        subir(List.of(JPEG), null);

        verifyNoInteractions(bloqueRepository);
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no sube nada")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> subir(List.of(JPEG), null)).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Si una foto es inválida no guarda nada ni el lote")
    void executeShouldNotStoreWhenFotoIsInvalid() {
        givenNegocio();

        assertThatThrownBy(() -> subir(List.of(JPEG, new byte[0]), null))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla el guardado de una foto borra la que ya había guardado y no guarda el lote")
    void executeShouldDeleteStoredFotosWhenStorageFails() {
        givenNegocio();
        doThrow(new IllegalStateException("disco lleno")).when(archivoCuadernoPort).guardar(any(), any(), eq(PNG));

        assertThatThrownBy(() -> subir(List.of(JPEG, PNG), null)).isInstanceOf(IllegalStateException.class);

        String guardadaJpeg = thenStoredClave(JPEG);
        String fallidaPng = thenStoredClave(PNG);
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, guardadaJpeg);
        verify(archivoCuadernoPort, never()).eliminar(NEGOCIO_ID, fallidaPng);
        verifyNoInteractions(loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla el guardado del lote borra del disco todas las fotos guardadas")
    void executeShouldDeleteStoredFotosWhenSaveFails() {
        givenNegocio();
        when(loteSubidaRepository.saveConFotos(any())).thenThrow(new IllegalStateException("base caída"));

        assertThatThrownBy(() -> subir(List.of(JPEG), null)).isInstanceOf(IllegalStateException.class);

        String guardada = thenStoredClave(JPEG);
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, guardada);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void givenRepositorySaves() {
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    // --- act ---
    private LoteItem subir(List<byte[]> fotos, BloqueId bloqueDestinoId) {
        return subirLote.execute(new SubirLoteRequest(USUARIO_ID, fotos, bloqueDestinoId));
    }

    // --- assert ---
    private String thenStoredClave(byte[] contenido) {
        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), clave.capture(), eq(contenido));
        return clave.getValue();
    }

    private void thenSavedLoteHasBloqueDestino() {
        ArgumentCaptor<LoteSubida> lote = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(lote.capture());
        assertThat(lote.getValue().getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    private void thenSavedLoteHasFotos(int cantidad) {
        ArgumentCaptor<LoteSubida> lote = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(lote.capture());
        assertThat(lote.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getValue().getFotos()).extracting(FotoCuaderno::getPosicion).hasSize(cantidad);
    }
}
