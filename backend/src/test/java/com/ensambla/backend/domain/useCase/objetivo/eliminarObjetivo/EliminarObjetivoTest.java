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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @BeforeEach
    void givenNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Borra el objetivo filtrando por el negocio de la sesión")
    void executeShouldDeleteForSessionNegocio() {
        givenObjetivoDelNegocio();

        whenEliminar();

        verify(objetivoRepository).deleteByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un objetivo ajeno o inexistente responde 404 y no se borra nada")
    void executeShouldNotDeleteWhenObjetivoIsNotOwned() {
        givenNingunObjetivoDelNegocio();

        assertThatThrownBy(this::whenEliminar).isInstanceOf(ResourceNotFoundException.class);

        verify(objetivoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }

    // --- arrange ---
    private void givenObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.of(objetivo()));
    }

    private void givenNingunObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }

    // --- act ---
    private void whenEliminar() {
        eliminarObjetivo.execute(new EliminarObjetivoRequest(USUARIO_ID, OBJETIVO_ID));
    }
}
