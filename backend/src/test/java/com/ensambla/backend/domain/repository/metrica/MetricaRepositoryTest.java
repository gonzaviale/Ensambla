package com.ensambla.backend.domain.repository.metrica;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricaRepositoryTest {

    private final MetricaRepository metricaRepository = mock(MetricaRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Obtiene una métrica que pertenece al cuaderno del negocio")
    void obtenerShouldReturnOwnedMetric() {
        Metrica propia = metrica(
                definicion(FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES),
                0);
        when(metricaRepository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(propia));

        Metrica result = metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID);

        assertThat(result.getId()).isEqualTo(METRICA_ID);
    }

    @Test
    @DisplayName("Una métrica inexistente o ajena produce el mismo 404")
    void obtenerShouldThrowWhenMissingOrForeign() {
        when(metricaRepository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Métrica no encontrada");
    }
}
