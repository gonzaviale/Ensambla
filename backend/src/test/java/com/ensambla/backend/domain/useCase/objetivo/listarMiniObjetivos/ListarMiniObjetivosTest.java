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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarMiniObjetivosTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private ListarMiniObjetivos listarMiniObjetivos;

    @BeforeEach
    void setUp() {
        listarMiniObjetivos = new ListarMiniObjetivos(negocioRepository, objetivoRepository, miniObjetivoRepository);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve los mini-objetivos del objetivo del negocio de la sesión")
    void executeShouldReturnTheMiniObjetivosOfTheObjetivo() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID))
                .thenReturn(List.of(miniObjetivo()));

        ListarMiniObjetivosResponse result = whenListar();

        assertThat(result.minis()).extracting(MiniObjetivoItem::id).containsExactly(MINI_ID);
        verify(miniObjetivoRepository).findAllByObjetivoIdAndNegocioId(OBJETIVO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente y no consulta sus mini-objetivos")
    void executeShouldFailWhenTheObjetivoBelongsToAnotherBusiness() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(this::whenListar)
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Objetivo no encontrado");
        verifyNoInteractions(miniObjetivoRepository);
    }

    // --- act ---
    private ListarMiniObjetivosResponse whenListar() {
        return listarMiniObjetivos.execute(new ListarMiniObjetivosRequest(USUARIO_ID, OBJETIVO_ID));
    }
}
