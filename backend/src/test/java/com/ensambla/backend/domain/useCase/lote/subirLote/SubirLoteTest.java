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
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;

import static com.ensambla.backend.support.LoteTestData.AHORA;
import static com.ensambla.backend.support.LoteTestData.BLOQUE_ID;
import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static com.ensambla.backend.support.LotesDePrueba.unArchivoVacio;
import static com.ensambla.backend.support.LotesDePrueba.unLoteConUnaFoto;
import static com.ensambla.backend.support.LotesDePrueba.unaFotoJpeg;
import static com.ensambla.backend.support.LotesDePrueba.unaFotoPng;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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

    private LoteItem lote;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Cada foto se guarda en la carpeta del negocio de la sesión y después se guarda el lote pendiente")
    void cadaFotoSeGuardaEnLaCarpetaDelNegocioYDespuesSeGuardaElLotePendiente() {
        dadoQueElLoteSeGuarda();

        cuandoSeSubenUnaFotoJpegYUnaPng();

        entoncesElLoteQuedaPendiente();
        entoncesCadaFotoSeGuardaEnLaCarpetaDelNegocio();
        entoncesSeControlanLasPaginasDelPlan();
        entoncesSeGuardaUnLoteDelNegocioConDosFotos();
    }

    @Test
    @DisplayName("Con un bloque destino del negocio se valida y se guarda en el lote")
    void conUnBloqueDestinoDelNegocioSeValidaYSeGuardaEnElLote() {
        dadoQueElLoteSeGuarda();

        cuandoSeSubeUnaFotoConDestino(BLOQUE_ID);

        entoncesSeValidaQueElBloqueDestinoSeaDelNegocio();
        entoncesElLoteGuardadoRecuerdaElBloqueDestino();
    }

    @Test
    @DisplayName("Un bloque destino de otro negocio responde como inexistente y no sube nada")
    void unBloqueDestinoDeOtroNegocioRespondeComoInexistenteYNoSubeNada() {
        dadoUnBloqueDestinoDeOtroNegocio();

        cuandoSeIntentaSubirUnaFotoConDestino(BLOQUE_ID);

        entoncesSeRechazaPorqueNoExiste();
        entoncesNoSeGuardaNingunArchivoNiLote();
    }

    @Test
    @DisplayName("Sin bloque destino no se consultan los bloques")
    void sinBloqueDestinoNoSeConsultanLosBloques() {
        dadoQueElLoteSeGuarda();

        cuandoSeSubeUnaFotoConDestino(null);

        entoncesNoSeConsultaNingunBloque();
    }

    @Test
    @DisplayName("Si el usuario no tiene negocio no se sube nada")
    void siElUsuarioNoTieneNegocioNoSeSubeNada() {
        dadoUnUsuarioSinNegocio();

        cuandoSeIntentaSubirUnaFotoConDestino(null);

        entoncesSeRechazaPorqueNoExiste();
        entoncesNoSeGuardaNingunArchivoNiLote();
    }

    @Test
    @DisplayName("Si una foto es inválida no se guarda nada ni el lote")
    void siUnaFotoEsInvalidaNoSeGuardaNadaNiElLote() {
        cuandoSeIntentaSubirUnaFotoYUnArchivoVacio();

        entoncesSeRechazaPorReglaDeNegocio();
        entoncesNoSeGuardaNingunArchivoNiLote();
    }

    @Test
    @DisplayName("Si falla el guardado de una foto se borra la que ya estaba guardada y no se guarda el lote")
    void siFallaElGuardadoDeUnaFotoSeBorraLaQueYaEstabaGuardadaYNoSeGuardaElLote() {
        dadoQueFallaElGuardadoDeLaFotoPng();

        cuandoSeIntentaSubirUnaFotoJpegYUnaPng();

        entoncesSeRechazaPorUnaFalla();
        entoncesSeBorraLaFotoJpegYNoLaPngQueFallo();
        entoncesNoSeGuardaElLote();
    }

    @Test
    @DisplayName("Si falla el guardado del lote se borran del disco todas las fotos guardadas")
    void siFallaElGuardadoDelLoteSeBorranDelDiscoTodasLasFotosGuardadas() {
        dadoQueFallaElGuardadoDelLote();

        cuandoSeIntentaSubirUnaFotoJpeg();

        entoncesSeRechazaPorUnaFalla();
        entoncesSeBorraLaFotoJpegGuardada();
    }

    // --- dado ---
    private void dadoQueElLoteSeGuarda() {
        when(loteSubidaRepository.saveConFotos(any())).thenReturn(unLoteConUnaFoto());
    }

    private void dadoUnBloqueDestinoDeOtroNegocio() {
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenThrow(new ResourceNotFoundException("Bloque no encontrado"));
    }

    private void dadoUnUsuarioSinNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));
    }

    private void dadoQueFallaElGuardadoDeLaFotoPng() {
        doThrow(new IllegalStateException("disco lleno"))
                .when(archivoCuadernoPort).guardar(any(), any(), eq(unaFotoPng()));
    }

    private void dadoQueFallaElGuardadoDelLote() {
        when(loteSubidaRepository.saveConFotos(any())).thenThrow(new IllegalStateException("base caída"));
    }

    // --- cuando ---
    private void cuandoSeSubenUnaFotoJpegYUnaPng() {
        lote = subir(List.of(unaFotoJpeg(), unaFotoPng()), null);
    }

    private void cuandoSeSubeUnaFotoConDestino(BloqueId bloqueDestinoId) {
        lote = subir(List.of(unaFotoJpeg()), bloqueDestinoId);
    }

    private void cuandoSeIntentaSubirUnaFotoConDestino(BloqueId bloqueDestinoId) {
        rechazo = catchThrowable(() -> cuandoSeSubeUnaFotoConDestino(bloqueDestinoId));
    }

    private void cuandoSeIntentaSubirUnaFotoJpeg() {
        rechazo = catchThrowable(() -> cuandoSeSubeUnaFotoConDestino(null));
    }

    private void cuandoSeIntentaSubirUnaFotoJpegYUnaPng() {
        rechazo = catchThrowable(this::cuandoSeSubenUnaFotoJpegYUnaPng);
    }

    private void cuandoSeIntentaSubirUnaFotoYUnArchivoVacio() {
        rechazo = catchThrowable(() -> subir(List.of(unaFotoJpeg(), unArchivoVacio()), null));
    }

    private LoteItem subir(List<byte[]> fotos, BloqueId bloqueDestinoId) {
        return subirLote.execute(new SubirLoteRequest(USUARIO_ID, fotos, bloqueDestinoId));
    }

    // --- entonces ---
    private void entoncesElLoteQuedaPendiente() {
        assertThat(lote.id()).isEqualTo(LOTE_ID);
        assertThat(lote.estado()).isEqualTo("PENDIENTE");
    }

    private void entoncesCadaFotoSeGuardaEnLaCarpetaDelNegocio() {
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(unaFotoJpeg()));
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), startsWith(NEGOCIO_ID + "/fotos/"), eq(unaFotoPng()));
    }

    private void entoncesSeControlanLasPaginasDelPlan() {
        verify(controlPlanService).validarPaginasIa(NEGOCIO_ID, 2, AHORA);
    }

    private void entoncesSeGuardaUnLoteDelNegocioConDosFotos() {
        LoteSubida guardado = loteGuardado();
        assertThat(guardado.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardado.getFotos()).extracting(FotoCuaderno::getPosicion).hasSize(2);
    }

    private void entoncesSeValidaQueElBloqueDestinoSeaDelNegocio() {
        verify(bloqueRepository).obtener(BLOQUE_ID, NEGOCIO_ID);
    }

    private void entoncesElLoteGuardadoRecuerdaElBloqueDestino() {
        assertThat(loteGuardado().getBloqueDestinoId()).isEqualTo(BLOQUE_ID);
    }

    private void entoncesNoSeConsultaNingunBloque() {
        verifyNoInteractions(bloqueRepository);
    }

    private void entoncesSeRechazaPorqueNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesSeRechazaPorReglaDeNegocio() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesSeRechazaPorUnaFalla() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }

    private void entoncesNoSeGuardaNingunArchivoNiLote() {
        verifyNoInteractions(archivoCuadernoPort, loteSubidaRepository);
    }

    private void entoncesNoSeGuardaElLote() {
        verifyNoInteractions(loteSubidaRepository);
    }

    private void entoncesSeBorraLaFotoJpegYNoLaPngQueFallo() {
        String claveDeLaJpeg = claveGuardadaDe(unaFotoJpeg());
        String claveDeLaPng = claveGuardadaDe(unaFotoPng());
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, claveDeLaJpeg);
        verify(archivoCuadernoPort, never()).eliminar(NEGOCIO_ID, claveDeLaPng);
    }

    private void entoncesSeBorraLaFotoJpegGuardada() {
        String claveDeLaJpeg = claveGuardadaDe(unaFotoJpeg());
        verify(archivoCuadernoPort).eliminar(NEGOCIO_ID, claveDeLaJpeg);
    }

    private LoteSubida loteGuardado() {
        ArgumentCaptor<LoteSubida> guardado = ArgumentCaptor.forClass(LoteSubida.class);
        verify(loteSubidaRepository).saveConFotos(guardado.capture());
        return guardado.getValue();
    }

    private String claveGuardadaDe(byte[] contenido) {
        ArgumentCaptor<String> clave = ArgumentCaptor.forClass(String.class);
        verify(archivoCuadernoPort).guardar(eq(NEGOCIO_ID), clave.capture(), eq(contenido));
        return clave.getValue();
    }
}
