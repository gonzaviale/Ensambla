package com.ensambla.backend.domain.useCase.metrica.eliminarMetrica;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarMetricaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final EliminarMetrica eliminarMetrica = new MetricaUseCaseConfig().eliminarMetrica(
            negocioRepository, metricaRepository, new TransaccionDirecta());

    private Throwable rechazo;

    @Test
    @DisplayName("Quita la métrica del cuaderno del negocio de la sesión")
    void quitaLaMetricaDelCuadernoDelNegocio() {
        dadoElNegocioDeLaSesion();

        cuandoSeQuitaLaMetrica();

        entoncesSeBuscoEnElCuadernoDelNegocio();
        entoncesSeQuito();
    }

    @Test
    @DisplayName("Una métrica ajena o inexistente responde 404 y no borra nada")
    void unaMetricaAjenaRespondeNoEncontradaYNoBorraNada() {
        dadoElNegocioDeLaSesion();
        dadaUnaMetricaAjena();

        cuandoSeIntentaQuitarLaMetrica();

        entoncesNoSeEncuentraLaMetrica();
        entoncesNoSeQuitoNada();
    }

    private void dadoElNegocioDeLaSesion() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private void dadaUnaMetricaAjena() {
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenThrow(new ResourceNotFoundException("Métrica no encontrada"));
    }

    private void cuandoSeQuitaLaMetrica() {
        eliminarMetrica.execute(new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID));
    }

    private void cuandoSeIntentaQuitarLaMetrica() {
        rechazo = catchThrowable(this::cuandoSeQuitaLaMetrica);
    }

    private void entoncesSeBuscoEnElCuadernoDelNegocio() {
        verify(metricaRepository).obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID);
    }

    private void entoncesSeQuito() {
        verify(metricaRepository).deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);
    }

    private void entoncesNoSeEncuentraLaMetrica() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeQuitoNada() {
        verify(metricaRepository, never()).deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);
    }
}
