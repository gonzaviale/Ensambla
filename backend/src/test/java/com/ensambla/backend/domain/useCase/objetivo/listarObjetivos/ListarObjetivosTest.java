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

    private List<ObjetivoItem> objetivos;

    @BeforeEach
    void dadoElNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los objetivos del negocio de la sesión en el orden del repositorio")
    void listaLosObjetivosDelNegocioDeLaSesionEnElOrdenDelRepositorio() {
        dadoDosObjetivosEnElNegocio();

        cuandoSeListan();

        entoncesSeListanEnElOrdenDelRepositorio();
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el negocio no tiene objetivos")
    void devuelveUnaListaVaciaCuandoElNegocioNoTieneObjetivos() {
        dadoUnNegocioSinObjetivos();

        cuandoSeListan();

        entoncesNoHayObjetivos();
    }

    // --- dado ---

    private void dadoDosObjetivosEnElNegocio() {
        Objetivo otro = objetivo();
        otro.setId(OTRO_OBJETIVO_ID);
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivo(), otro));
    }

    private void dadoUnNegocioSinObjetivos() {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    // --- cuando ---

    private void cuandoSeListan() {
        objetivos = listarObjetivos.execute(new ListarObjetivosRequest(USUARIO_ID));
    }

    // --- entonces ---

    private void entoncesSeListanEnElOrdenDelRepositorio() {
        assertThat(objetivos).extracting(ObjetivoItem::id).containsExactly(OBJETIVO_ID, OTRO_OBJETIVO_ID);
        verify(objetivoRepository).findAllByNegocioId(NEGOCIO_ID);
    }

    private void entoncesNoHayObjetivos() {
        assertThat(objetivos).isEmpty();
    }
}
