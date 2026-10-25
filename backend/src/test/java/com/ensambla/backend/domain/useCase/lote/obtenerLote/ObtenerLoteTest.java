package com.ensambla.backend.domain.useCase.lote.obtenerLote;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.lote.LoteSubidaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.useCase.lote.LoteItem;
import com.ensambla.backend.support.LoteTestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.LoteTestData.LOTE_ID;
import static com.ensambla.backend.support.LoteTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.LoteTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerLoteTest {

    private static final ObtenerLoteRequest REQUEST = new ObtenerLoteRequest(USUARIO_ID, LOTE_ID);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final LoteSubidaRepository loteSubidaRepository = mock(LoteSubidaRepository.class);

    private ObtenerLote obtenerLote;

    @BeforeEach
    void setUp() {
        obtenerLote = new ObtenerLote(negocioRepository,
                loteSubidaRepository);
    }

    @Test
    @DisplayName("Busca el lote en el negocio de la sesión y devuelve sus fotos")
    void executeShouldReturnLoteOfSessionNegocio() {
        givenNegocio();
        givenLote();

        LoteItem result = obtenerLote.execute(REQUEST);

        assertThat(result.id()).isEqualTo(LOTE_ID);
        assertThat(result.fotos()).hasSize(1);
    }

    @Test
    @DisplayName("Propaga el 404 de un lote de otro negocio")
    void executeShouldPropagateNotFound() {
        givenNegocio();
        givenLoteAjeno();

        assertThatThrownBy(() -> obtenerLote.execute(REQUEST)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void givenLote() {
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID))
                .thenReturn(LoteTestData.lote(List.of(LoteTestData.foto())));
    }

    private void givenLoteAjeno() {
        when(loteSubidaRepository.obtener(NEGOCIO_ID, LOTE_ID))
                .thenThrow(new ResourceNotFoundException("Lote no encontrado"));
    }
}
