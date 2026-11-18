package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static com.ensambla.backend.support.LotesDePrueba.unLoteConUnaFoto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerLoteTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);
    private final ObtenerLote obtenerLote = new ObtenerLote(negocioRepository, loteSubidaRepository);

    private LoteItem lote;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("El lote se busca en el negocio de la sesión y se devuelve con sus fotos")
    void elLoteSeBuscaEnElNegocioDeLaSesionYSeDevuelveConSusFotos() {
        dadoUnLoteDelNegocio();

        cuandoSeObtieneElLote();

        entoncesSeDevuelveElLoteConSusFotos();
    }

    @Test
    @DisplayName("Un lote de otro negocio se informa como no encontrado")
    void unLoteDeOtroNegocioSeInformaComoNoEncontrado() {
        dadoUnLoteDeOtroNegocio();

        cuandoSeIntentaObtenerElLote();

        entoncesSeRechazaPorqueElLoteNoExiste();
    }

    // --- dado ---
    private void dadoUnLoteDelNegocio() {
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(unLoteConUnaFoto());
    }

    private void dadoUnLoteDeOtroNegocio() {
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID))
                .thenThrow(new ResourceNotFoundException("Lote no encontrado"));
    }

    // --- cuando ---
    private void cuandoSeObtieneElLote() {
        lote = obtenerLote.execute(new ObtenerLoteRequest(USUARIO_ID, LOTE_ID));
    }

    private void cuandoSeIntentaObtenerElLote() {
        rechazo = catchThrowable(this::cuandoSeObtieneElLote);
    }

    // --- entonces ---
    private void entoncesSeDevuelveElLoteConSusFotos() {
        assertThat(lote.id()).isEqualTo(LOTE_ID);
        assertThat(lote.fotos()).hasSize(1);
    }

    private void entoncesSeRechazaPorqueElLoteNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
