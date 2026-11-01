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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LoteSubidaRepositoryTest {

    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Devuelve el lote del negocio con sus fotos")
    void obtenerShouldReturnOwnedLote() {
        givenLoteExists();

        LoteSubida result = loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID);

        assertThat(result.getId()).isEqualTo(LOTE_ID);
        assertThat(result.getFotos()).hasSize(1);
    }

    @Test
    @DisplayName("Un lote de otro negocio responde como inexistente")
    void obtenerShouldThrowWhenLoteBelongsToOtherNegocio() {
        givenLoteMissing();

        assertThatThrownBy(() -> loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Lote no encontrado");
    }

    // --- arrange ---
    private void givenLoteExists() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(LoteTestData.lote(List.of(LoteTestData.foto()))));
    }

    private void givenLoteMissing() {
        when(loteSubidaRepository.findByIdAndNegocioId(LOTE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());
    }
}
