package com.ensambla.backend.domain.repository.lote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoteSubidaRepositoryTest {

    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class, CALLS_REAL_METHODS);

    private LoteSubida obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve el lote del negocio con sus fotos")
    void devuelveElLoteDelNegocioConSusFotos() {
        dadoUnLoteDelNegocioConUnaFoto();

        cuandoSeObtieneElLote();

        entoncesSeDevuelveElLoteConSuFoto();
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente")
    void unLoteDeOtroNegocioRespondeComoInexistente() {
        dadoQueElLoteNoEsDelNegocio();

        cuandoSeIntentaObtenerElLote();

        entoncesSeRespondeQueElLoteNoSeEncontro();
    }

    // --- dado ---

    private void dadoUnLoteDelNegocioConUnaFoto() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    private void dadoQueElLoteNoEsDelNegocio() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElLote() {
        obtenido = loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID);
    }

    private void cuandoSeIntentaObtenerElLote() {
        rechazo = catchThrowable(this::cuandoSeObtieneElLote);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElLoteConSuFoto() {
        assertThat(obtenido.getId()).isEqualTo(LOTE_ID);
        assertThat(obtenido.getFotos()).hasSize(1);
    }

    private void entoncesSeRespondeQueElLoteNoSeEncontro() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Lote no encontrado");
    }
}
