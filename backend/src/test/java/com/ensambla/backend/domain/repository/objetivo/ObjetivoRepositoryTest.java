package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjetivoRepositoryTest {

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene un objetivo que pertenece al negocio")
    void obtenerShouldReturnOwnedObjetivo() {
        givenObjetivoDelNegocio();

        Objetivo result = objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID);

        assertThat(result.getId()).isEqualTo(OBJETIVO_ID);
    }

    @Test
    @DisplayName("Un objetivo inexistente o ajeno produce el mismo 404")
    void obtenerShouldThrowWhenMissingOrForeign() {
        givenNingunObjetivoDelNegocio();

        assertThatThrownBy(() -> objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
    }

    // --- arrange ---
    private void givenObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.of(objetivo()));
    }

    private void givenNingunObjetivoDelNegocio() {
        when(objetivoRepository.findByIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }
}
