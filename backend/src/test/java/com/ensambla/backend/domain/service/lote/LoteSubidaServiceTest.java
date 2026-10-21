package com.ensambla.backend.domain.service.lote;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.LecturaCuadernoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.ArchivoFoto;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.PNG;
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

    private static final UUID BLOQUE_ID = UUID.fromString("00000000-0000-4000-8000-000000000015");

    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class);
    private final LoteSubidaService loteSubidaService =
            new LoteSubidaService(loteSubidaRepository, archivoCuadernoPort, mock(ControlPlanService.class));

    @Test
    @DisplayName("Guarda cada foto en la carpeta del negocio y después guarda el lote pendiente")
    void subirShouldStoreFotosAndSaveLote() {
        givenRepositorySaves();

        LoteSubida result = loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG, PNG), null, AHORA);

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(JPEG));
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(PNG));
        thenSavedLoteHasFotos(2);
    }

    @Test
    @DisplayName("Guarda el lote con el bloque destino que eligió la persona")
    void subirShouldSaveLoteWithBloqueDestino() {
        givenRepositorySaves();

        loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG), BLOQUE_ID, AHORA);

        thenSavedLoteHasBloqueDestino(BLOQUE_ID);
    }

    @Test
    @DisplayName("Si una foto es inválida no guarda nada ni el lote")
    void subirShouldNotStoreWhenFotoIsInvalid() {
        List<byte[]> contenidos = List.of(JPEG, new byte[0]);

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, contenidos, null, AHORA))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla el guardado de una foto borra la que ya había guardado y no guarda el lote")
    void subirShouldDeleteStoredFotosWhenStorageFails() {
        givenStorageFailsOn(PNG);

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG, PNG), null, AHORA))
                .isInstanceOf(IllegalStateException.class);
        String guardadaJpeg = thenStoredClave(JPEG);
        String fallidaPng = thenStoredClave(PNG);
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, guardadaJpeg);
        verify(archivoCuadernoPort, never()).eliminar(NEGOCIO_ID, fallidaPng);
        verifyNoInteractions(loteSubidaRepository);
    }

    @Test
    @DisplayName("Si falla el guardado del lote borra del disco todas las fotos guardadas")
    void subirShouldDeleteStoredFotosWhenSaveFails() {
        givenRepositoryFails();

        assertThatThrownBy(() -> loteSubidaService.subir(NEGOCIO_ID, List.of(JPEG), null, AHORA))
                .isInstanceOf(IllegalStateException.class);
        String guardada = thenStoredClave(JPEG);
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, guardada);
    }

    @Test
    @DisplayName("Confirma el lote vinculándolo al bloque donde quedaron los datos")
    void confirmarShouldMarkLoteAsConfirmed() {
        when(loteSubidaRepository.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA)).thenReturn(true);

        loteSubidaService.confirmar(LoteTestData.lote(List.of()), BLOQUE_ID, AHORA);

        verify(loteSubidaRepository).confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA);
    }

    @Test
    @DisplayName("Rechaza la confirmación si otra se adelantó")
    void confirmarShouldThrowWhenLoteWasAlreadyConfirmed() {
        when(loteSubidaRepository.confirmar(LOTE_ID, NEGOCIO_ID, BLOQUE_ID, AHORA)).thenReturn(false);
        LoteSubida lote = LoteTestData.lote(List.of());

        assertThatThrownBy(() -> loteSubidaService.confirmar(lote, BLOQUE_ID, AHORA))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este cuaderno ya se guardó");
    }

    @Test
    @DisplayName("Lee los bytes de una foto del lote del negocio junto con su tipo")
    void leerFotoDeLoteShouldReturnBytesAndType() {
        givenLoteExists();
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE)).thenReturn(JPEG);

        ArchivoFoto result = loteSubidaService.leerFotoDeLote(NEGOCIO_ID, LOTE_ID, FOTO_ID);

        assertThat(result.contenido()).isEqualTo(JPEG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Una foto que no es del lote responde como inexistente sin leer el archivo")
    void leerFotoDeLoteShouldThrowWhenFotoIsNotInLote() {
        givenLoteExists();
        UUID otraFoto = UUID.fromString("00000000-0000-4000-8000-000000000099");

        assertThatThrownBy(() -> loteSubidaService.leerFotoDeLote(NEGOCIO_ID, LOTE_ID, otraFoto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Foto no encontrada");
        verifyNoInteractions(archivoCuadernoPort);
    }

    @Test
    @DisplayName("Lee los bytes de una foto del negocio por su id")
    void leerFotoShouldReturnBytesAndType() {
        when(loteSubidaRepository.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.foto()));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE)).thenReturn(PNG);

        ArchivoFoto result = loteSubidaService.leerFoto(NEGOCIO_ID, FOTO_ID);

        assertThat(result.contenido()).isEqualTo(PNG);
        assertThat(result.tipoContenido()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("Una foto registrada cuyo archivo falta responde como no disponible")
    void leerFotoShouldThrowNotFoundWhenFileIsMissing() {
        when(loteSubidaRepository.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.foto()));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, LoteTestData.CLAVE))
                .thenThrow(new LecturaCuadernoException("No se pudo acceder a la foto privada del lote"));

        assertThatThrownBy(() -> loteSubidaService.leerFoto(NEGOCIO_ID, FOTO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("La foto ya no está disponible");
    }

    @Test
    @DisplayName("Una foto de otro negocio responde como inexistente sin leer el archivo")
    void leerFotoShouldThrowWhenFotoBelongsToOtherNegocio() {
        when(loteSubidaRepository.findFotoByIdAndNegocioId(FOTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loteSubidaService.leerFoto(NEGOCIO_ID, FOTO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Foto no encontrada");
        verifyNoInteractions(archivoCuadernoPort);
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
    private void givenRepositorySaves() {
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    private void givenRepositoryFails() {
        when(loteSubidaRepository.saveConFotos(any())).thenThrow(new IllegalStateException("base caída"));
    }

    private void givenStorageFailsOn(byte[] contenido) {
        doThrow(new IllegalStateException("disco lleno"))
                .when(archivoCuadernoPort).guardar(any(), any(), eq(contenido));
    }

    private void givenLoteExists() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    private void givenLoteMissing() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- assert ---
    private String thenStoredClave(byte[] contenido) {
        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), clave.capture(), eq(contenido));
        return clave.getValue();
    }

    private void thenSavedLoteHasBloqueDestino(UUID bloqueId) {
        ArgumentCaptor<LoteSubida> lote = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(lote.capture());
        assertThat(lote.getValue().getBloqueDestinoId()).isEqualTo(bloqueId);
    }

    private void thenSavedLoteHasFotos(int cantidad) {
        ArgumentCaptor<LoteSubida> lote = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(lote.capture());
        assertThat(lote.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(lote.getValue().getFotos()).extracting(FotoCuaderno::getPosicion).hasSize(cantidad);
    }
}
