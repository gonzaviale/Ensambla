package com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.objetivo.EstadoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.objetivo.MiniObjetivoRepository;
import com.ensambla.backend.domain.repository.objetivo.ObjetivoRepository;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.MiniObjetivoTestData.MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AbandonarMiniObjetivoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ObjetivoRepository objetivoRepository = mock(ObjetivoRepository.class);
    private final MiniObjetivoRepository miniObjetivoRepository = mock(MiniObjetivoRepository.class);
    private AbandonarMiniObjetivo abandonarMiniObjetivo;

    @BeforeEach
    void setUp() {
        abandonarMiniObjetivo = new AbandonarMiniObjetivo(
                negocioRepository,
                objetivoRepository,
                miniObjetivoRepository,
                new TransaccionDirecta(),
                Clock.fixed(AHORA.plusDays(1).toInstant(), ZoneOffset.UTC));
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID)).thenReturn(objetivo());
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(miniObjetivo());
    }

    @Test
    @DisplayName("Deja el mini-objetivo abandonado, con la fecha de hoy, y lo guarda")
    void executeShouldSaveTheMiniObjetivoAsAbandoned() {
        whenAbandonar();

        ArgumentCaptor<MiniObjetivo> captor = ArgumentCaptor.forClass(MiniObjetivo.class);
        verify(miniObjetivoRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(MINI_ID);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(captor.getValue().getActualizadoEn()).isEqualTo(AHORA.plusDays(1));
    }

    @Test
    @DisplayName("Uno que ya no está en curso no se puede quitar y no se guarda nada")
    void executeShouldFailWhenTheMiniObjetivoIsNotInProgress() {
        MiniObjetivo reemplazado = miniObjetivo();
        reemplazado.setEstado(EstadoMiniObjetivo.REEMPLAZADO);
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID)).thenReturn(reemplazado);

        assertThatThrownBy(this::whenAbandonar).isInstanceOf(BusinessRuleException.class);
        verify(miniObjetivoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un objetivo de otro negocio responde como inexistente")
    void executeShouldFailWhenTheObjetivoBelongsToAnotherBusiness() {
        when(objetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID))
                .thenThrow(new ResourceNotFoundException("Objetivo no encontrado"));

        assertThatThrownBy(this::whenAbandonar).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(miniObjetivoRepository);
    }

    @Test
    @DisplayName("Un mini-objetivo de otro objetivo responde como inexistente")
    void executeShouldFailWhenTheMiniObjetivoBelongsToAnotherObjetivo() {
        when(miniObjetivoRepository.obtener(NEGOCIO_ID, OBJETIVO_ID, MINI_ID))
                .thenThrow(new ResourceNotFoundException("Mini-objetivo no encontrado"));

        assertThatThrownBy(this::whenAbandonar).isInstanceOf(ResourceNotFoundException.class);
        verify(miniObjetivoRepository, never()).save(any());
    }

    // --- act ---
    private void whenAbandonar() {
        abandonarMiniObjetivo.execute(new AbandonarMiniObjetivoRequest(USUARIO_ID, OBJETIVO_ID, MINI_ID));
    }
}
