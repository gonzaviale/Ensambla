package com.ensambla.backend.domain.useCase.objetivo.listarObjetivos;

import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.domain.useCase.objetivo.ObjetivoItem;
import com.ensambla.backend.infrastructure.config.useCase.ObjetivoUseCaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListarObjetivosTest {

    private static final ObjetivoId OTRO_OBJETIVO_ID = ObjetivoId.de("00000000-0000-4000-8000-000000000006");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final ListarObjetivos listarObjetivos = new ObjetivoUseCaseConfig().listarObjetivos(
            negocioRepository,
            objetivoRepository);

    @BeforeEach
    void givenNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los objetivos del negocio de la sesión en el orden del repositorio")
    void executeShouldReturnObjetivosOfSessionNegocio() {
        givenObjetivos(objetivo(), otroObjetivo());

        List<ObjetivoItem> result = whenListar();

        assertThat(result).extracting(ObjetivoItem::id).containsExactly(OBJETIVO_ID, OTRO_OBJETIVO_ID);
        verify(objetivoRepository).findAllByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el negocio no tiene objetivos")
    void executeShouldReturnEmptyListWhenNoObjetivos() {
        givenObjetivos();

        List<ObjetivoItem> result = whenListar();

        assertThat(result).isEmpty();
    }

    // --- arrange ---
    private void givenObjetivos(Objetivo... objetivos) {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivos));
    }

    private Objetivo otroObjetivo() {
        Objetivo otro = objetivo();
        otro.setId(OTRO_OBJETIVO_ID);
        return otro;
    }

    // --- act ---
    private List<ObjetivoItem> whenListar() {
        return listarObjetivos.execute(new ListarObjetivosRequest(USUARIO_ID));
    }
}
