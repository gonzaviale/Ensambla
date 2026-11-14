package com.ensambla.backend.domain.useCase.metrica.eliminarMetrica;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarMetricaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final EliminarMetrica eliminarMetrica = new MetricaUseCaseConfig().eliminarMetrica(
            negocioRepository, metricaRepository, new TransaccionDirecta());

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Quita la métrica del cuaderno del negocio de la sesión")
    void executeShouldDeleteTheMetric() {
        eliminarMetrica.execute(request());

        verify(metricaRepository).obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID);
        verify(metricaRepository).deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Una métrica ajena o inexistente responde 404 y no borra nada")
    void executeShouldPropagateNotFound() {
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenThrow(new ResourceNotFoundException("Métrica no encontrada"));

        assertThatThrownBy(() -> eliminarMetrica.execute(request())).isInstanceOf(ResourceNotFoundException.class);
        verify(metricaRepository, never()).deleteByIdAndNegocioId(METRICA_ID, NEGOCIO_ID);
    }

    private static EliminarMetricaRequest request() {
        return new EliminarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID);
    }
}
