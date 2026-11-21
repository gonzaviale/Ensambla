package com.ensambla.backend.domain.repository.procesamiento;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.lote.EstadoLote;
import com.ensambla.backend.domain.model.procesamiento.EstadoLectura;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProcesamientoTestData.LOTE_ID;
import static com.ensambla.backend.support.ProcesamientoTestData.NEGOCIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcesamientoRepositoryTest {

    private final ProcesamientoRepository procesamientoRepository = mock(ProcesamientoRepository.class,
            CALLS_REAL_METHODS);

    private EstadoLectura estado;
    private EstadoLectura obtenido;
    private Throwable rechazo;

    @Test
    @DisplayName("Devuelve el estado del lote del negocio")
    void devuelveElEstadoDelLoteDelNegocio() {
        dadoUnLoteDelNegocioEnError();

        cuandoSeObtieneElEstado();

        entoncesSeDevuelveElEstadoDelLote();
    }

    @Test
    @DisplayName("Un lote ajeno o inexistente responde como no encontrado")
    void unLoteAjenoOInexistenteRespondeComoNoEncontrado() {
        dadoQueElLoteNoExisteOEsAjeno();

        cuandoSeIntentaObtenerElEstado();

        entoncesSeRespondeQueElLoteNoSeEncontro();
    }

    // --- dado ---

    private void dadoUnLoteDelNegocioEnError() {
        estado = new EstadoLectura(LOTE_ID, EstadoLote.ERROR, "Error", 3, null);
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.of(estado));
    }

    private void dadoQueElLoteNoExisteOEsAjeno() {
        when(procesamientoRepository.obtener(NEGOCIO_ID, LOTE_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeObtieneElEstado() {
        obtenido = procesamientoRepository.obtenerEstado(NEGOCIO_ID, LOTE_ID);
    }

    private void cuandoSeIntentaObtenerElEstado() {
        rechazo = catchThrowable(this::cuandoSeObtieneElEstado);
    }

    // --- entonces ---

    private void entoncesSeDevuelveElEstadoDelLote() {
        assertThat(obtenido).isEqualTo(estado);
    }

    private void entoncesSeRespondeQueElLoteNoSeEncontro() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Lote no encontrado");
    }
}
