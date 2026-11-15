package com.ensambla.backend.domain.service.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.TipoDecision;
import com.ensambla.backend.domain.repository.objetivo.DecisionObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.SEMANA;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class DecisionesObjetivoServiceTest {

    private static final ObjetivoId OTRO_OBJETIVO_ID = ObjetivoId.de("00000000-0000-4000-8000-0000000000a5");

    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private final DecisionObjetivoRepository decisionObjetivoRepository = mock(DecisionObjetivoRepository.class);
    private DecisionesObjetivoService service;

    @BeforeEach
    void setUp() {
        service = new DecisionesObjetivoService(objetivoRepository, miniObjetivoRepository, decisionObjetivoRepository);
    }

    @Test
    @DisplayName("Anota la decisión en el objetivo y la vincula al mini-objetivo en curso del producto del aviso")
    void registrarAvisoShouldLinkTheMiniObjetivoOfTheProduct() {
        givenObjetivos(objetivo());
        givenMinis(OBJETIVO_ID, miniObjetivo());

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        DecisionObjetivo guardada = decisionesGuardadas().getFirst();
        assertThat(guardada.objetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(guardada.miniObjetivoId()).isEqualTo(MINI_ID);
        assertThat(guardada.tipo()).isEqualTo(TipoDecision.AVISO_APLICADO);
        assertThat(guardada.avisoId()).isEqualTo(AVISO_ID);
        assertThat(guardada.registroId()).isEqualTo(PRODUCTO_ID);
        assertThat(guardada.semanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Guarda una decisión por cada objetivo activo del negocio")
    void registrarAvisoShouldSaveOneDecisionPerActiveObjetivo() {
        Objetivo otro = objetivo();
        otro.setId(OTRO_OBJETIVO_ID);
        givenObjetivos(objetivo(), otro);
        givenMinis(OBJETIVO_ID, miniObjetivo());
        givenMinis(OTRO_OBJETIVO_ID);

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        assertThat(decisionesGuardadas()).extracting(DecisionObjetivo::objetivoId)
                .containsExactly(OBJETIVO_ID, OTRO_OBJETIVO_ID);
        assertThat(decisionesGuardadas()).extracting(DecisionObjetivo::miniObjetivoId)
                .containsExactly(MINI_ID, null);
    }

    @Test
    @DisplayName("No anota nada en un objetivo que ya llegó a su meta")
    void registrarAvisoShouldSkipAchievedObjetivos() {
        Objetivo alcanzado = objetivo();
        alcanzado.marcarAlcanzado(AHORA);
        givenObjetivos(alcanzado);

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        verify(decisionObjetivoRepository, never()).save(any());
        verifyNoInteractions(miniObjetivoRepository);
    }

    @Test
    @DisplayName("Sin objetivos no guarda ninguna decisión")
    void registrarAvisoShouldSaveNothingWithoutObjetivos() {
        givenObjetivos();

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        verify(decisionObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un mini-objetivo que ya no está en curso no se vincula")
    void registrarAvisoShouldNotLinkAMiniObjetivoThatIsNoLongerInProgress() {
        MiniObjetivo logrado = miniObjetivo();
        logrado.setEstado(EstadoMiniObjetivo.LOGRADO);
        givenObjetivos(objetivo());
        givenMinis(OBJETIVO_ID, logrado);

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        assertThat(decisionesGuardadas().getFirst().miniObjetivoId()).isNull();
    }

    @Test
    @DisplayName("Un mini-objetivo de otro producto no se vincula")
    void registrarAvisoShouldNotLinkAMiniObjetivoOfAnotherProduct() {
        MiniObjetivo deOtroProducto = miniObjetivo();
        deOtroProducto.setId(OTRO_MINI_ID);
        deOtroProducto.setRegistroId(null);
        givenObjetivos(objetivo());
        givenMinis(OBJETIVO_ID, deOtroProducto);

        whenRegistrar(aviso(TipoAviso.ESTANCADO, EstadoAviso.APLICADO), TipoDecision.AVISO_APLICADO);

        assertThat(decisionesGuardadas().getFirst().miniObjetivoId()).isNull();
    }

    @Test
    @DisplayName("Un aviso que no es de un producto se anota sin vincular y sin buscar mini-objetivos")
    void registrarAvisoShouldNotLookForMiniObjetivosWhenTheAvisoIsNotAboutAProduct() {
        Aviso deLaZona = aviso(TipoAviso.CONSUMO_ZONA, EstadoAviso.DESCARTADO);
        deLaZona.setEntidad("zona");
        givenObjetivos(objetivo());

        whenRegistrar(deLaZona, TipoDecision.AVISO_DESCARTADO);

        assertThat(decisionesGuardadas().getFirst().miniObjetivoId()).isNull();
        assertThat(decisionesGuardadas().getFirst().tipo()).isEqualTo(TipoDecision.AVISO_DESCARTADO);
        verifyNoInteractions(miniObjetivoRepository);
    }

    // --- arrange ---
    private void givenObjetivos(Objetivo... objetivos) {
        when(objetivoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(objetivos));
    }

    private void givenMinis(ObjetivoId objetivoId, MiniObjetivo... minis) {
        when(miniObjetivoRepository.findAllByObjetivoIdAndNegocioId(objetivoId, NEGOCIO_ID))
                .thenReturn(List.of(minis));
    }

    // --- act ---
    private void whenRegistrar(Aviso aviso, TipoDecision tipo) {
        service.registrarAviso(aviso, tipo, AHORA);
    }

    // --- assert ---
    private List<DecisionObjetivo> decisionesGuardadas() {
        ArgumentCaptor<DecisionObjetivo> captor = ArgumentCaptor.forClass(DecisionObjetivo.class);
        verify(decisionObjetivoRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
}
