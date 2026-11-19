package com.ensambla.backend.domain.useCase.lote.obtenerFotoLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.port.ArchivoCuadernoPort;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.FotoArchivoResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.CLAVE;
import static com.ensambla.backend.support.LoteTestData.FOTO_ID;
import static com.ensambla.backend.support.LoteTestData.JPEG;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static com.ensambla.backend.support.LotesDePrueba.unLoteConUnaFoto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerFotoLoteTest {

    private static final FotoId FOTO_AJENA_ID = FotoId.de("00000000-0000-4000-8000-000000000099");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class, CALLS_REAL_METHODS);
    private final ArchivoCuadernoPort archivoCuadernoPort = mock(ArchivoCuadernoPort.class, CALLS_REAL_METHODS);
    private final ObtenerFotoLote obtenerFotoLote = new ObtenerFotoLote(
            negocioRepository, archivoCuadernoPort, loteSubidaRepository);

    private FotoArchivoResponse foto;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Se devuelven los bytes y el tipo de la foto del lote del negocio de la sesión")
    void seDevuelvenLosBytesYElTipoDeLaFotoDelLoteDelNegocioDeLaSesion() {
        dadoUnLoteDelNegocioConSuFotoGuardada();

        cuandoSePideLaFoto(FOTO_ID);

        entoncesSeDevuelvenLosBytesYElTipoDeLaFoto();
    }

    @Test
    @DisplayName("Una foto que no es del lote responde como inexistente sin leer el archivo")
    void unaFotoQueNoEsDelLoteRespondeComoInexistenteSinLeerElArchivo() {
        dadoUnLoteDelNegocioConSuFotoGuardada();

        cuandoSeIntentaPedirLaFoto(FOTO_AJENA_ID);

        entoncesSeRechazaPorqueNoExiste("Foto no encontrada");
        entoncesNoSeLeeNingunArchivo();
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente sin leer el archivo")
    void unLoteDeOtroNegocioRespondeComoInexistenteSinLeerElArchivo() {
        dadoUnLoteDeOtroNegocio();

        cuandoSeIntentaPedirLaFoto(FOTO_ID);

        entoncesSeRechazaPorqueNoExiste("Lote no encontrado");
        entoncesNoSeLeeNingunArchivo();
    }

    // --- dado ---
    private void dadoUnLoteDelNegocioConSuFotoGuardada() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(unLoteConUnaFoto()));
        when(archivoCuadernoPort.leer(NEGOCIO_ID, CLAVE)).thenReturn(JPEG);
    }

    private void dadoUnLoteDeOtroNegocio() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---
    private void cuandoSePideLaFoto(FotoId fotoId) {
        foto = obtenerFotoLote.execute(new ObtenerFotoLoteRequest(USUARIO_ID, LOTE_ID, fotoId));
    }

    private void cuandoSeIntentaPedirLaFoto(FotoId fotoId) {
        rechazo = catchThrowable(() -> cuandoSePideLaFoto(fotoId));
    }

    // --- entonces ---
    private void entoncesSeDevuelvenLosBytesYElTipoDeLaFoto() {
        assertThat(foto.contenido()).isEqualTo(JPEG);
        assertThat(foto.tipoContenido()).isEqualTo("image/jpeg");
    }

    private void entoncesSeRechazaPorqueNoExiste(String mensaje) {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage(mensaje);
    }

    private void entoncesNoSeLeeNingunArchivo() {
        verifyNoInteractions(archivoCuadernoPort);
    }
}
