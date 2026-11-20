package com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.infrastructure.config.useCase.ObjetivoUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarObjetivoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class, CALLS_REAL_METHODS);
    private final EliminarObjetivo eliminarObjetivo = new ObjetivoUseCaseConfig().eliminarObjetivo(
            negocioRepository,
            objetivoRepository,
            new TransaccionDirecta());

    private Throwable rechazo;

    @BeforeEach
    void dadoElNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Borra el objetivo filtrando por el negocio de la sesión")
    void borraElObjetivoFiltrandoPorElNegocioDeLaSesion() {
        dadoUnObjetivoDelNegocio();

        cuandoSeElimina();

        entoncesSeBorraElObjetivoDelNegocio();
    }

    @Test
    @DisplayName("Un objetivo ajeno o inexistente responde 404 y no se borra nada")
    void unObjetivoAjenoOInexistenteRespondeNoEncontradoYNoSeBorraNada() {
        dadoNingunObjetivoDelNegocio();

        cuandoSeIntentaEliminar();

        entoncesSeRechazaComoInexistenteSinBorrar();
    }

    // --- dado ---

    private void dadoUnObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.of(objetivo()));
    }

    private void dadoNingunObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- cuando ---

    private void cuandoSeElimina() {
        eliminarObjetivo.execute(new EliminarObjetivoRequest(USUARIO_ID, OBJETIVO_ID));
    }

    private void cuandoSeIntentaEliminar() {
        rechazo = catchThrowable(this::cuandoSeElimina);
    }

    // --- entonces ---

    private void entoncesSeBorraElObjetivoDelNegocio() {
        verify(objetivoRepository).deleteByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    private void entoncesSeRechazaComoInexistenteSinBorrar() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
        verify(objetivoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }
}
