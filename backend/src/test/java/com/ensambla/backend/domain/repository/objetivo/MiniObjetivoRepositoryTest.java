package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MiniObjetivoRepositoryTest {

    private final MiniObjetivoRepository miniObjetivoRepository =
            mock(MiniObjetivoRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene un mini-objetivo que pertenece al objetivo del negocio")
    void obtenerShouldReturnOwnedMiniObjetivo() {
        when(miniObjetivoRepository.findByIdAndObjetivoIdAndNegocioId(MINI_ID, OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(miniObjetivo()));

        MiniObjetivo result = miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID);

        assertThat(result.getId()).isEqualTo(MINI_ID);
    }

    @Test
    @DisplayName("Un mini-objetivo inexistente o ajeno produce el mismo 404")
    void obtenerShouldThrowWhenMissingOrForeign() {
        when(miniObjetivoRepository.findByIdAndObjetivoIdAndNegocioId(MINI_ID, OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Mini-objetivo no encontrado");
    }
}
