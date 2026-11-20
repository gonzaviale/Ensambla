package com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.MiniObjetivoItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarMiniObjetivosTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private ListarMiniObjetivos listarMiniObjetivos;

    private ListarMiniObjetivosResponse respuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        listarMiniObjetivos = new ListarMiniObjetivos(negocioRepository, objetivoRepository, miniObjetivoRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve los mini-objetivos del objetivo del negocio de la sesión")
    void devuelveLosMiniObjetivosDelObjetivoDelNegocioDeLaSesion() {
        dadoUnObjetivoConUnMiniObjetivo();

        cuandoSeListan();

        entoncesSeDevuelveElMiniObjetivoDelObjetivo();
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus mini-objetivos")
    void unObjetivoDeOtroNegocioRespondeComoInexistenteYNoConsultaSusMiniObjetivos() {
        dadoUnObjetivoDeOtroNegocio();

        cuandoSeIntentaListar();

        entoncesSeRechazaComoInexistenteSinConsultarMiniObjetivos();
    }

    // --- dado ---

    private void dadoUnObjetivoConUnMiniObjetivo() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));
    }

    private void dadoUnObjetivoDeOtroNegocio() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeListan() {
        respuesta = listarMiniObjetivos.execute(new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeIntentaListar() {
        rechazo = catchThrowable(this::cuandoSeListan);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElMiniObjetivoDelObjetivo() {
        assertThat(respuesta.minis()).extracting(MiniObjetivoItem::id).containsExactly(MINI_ID);
        verify(miniObjetivoRepository).findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void entoncesSeRechazaComoInexistenteSinConsultarMiniObjetivos() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Objetivo no encontrado");
        verifyNoInteractions(miniObjetivoRepository);
    }
}
