package com.ensambla.backend.domain.repository.metrica;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.Metrica;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MetricaRepositoryTest {

    private final MetricaRepository metricaRepository = mock(MetricaRepository.class, CALLS_REAL_METHODS);

    private Metrica encontrada;

    private Throwable rechazo;

    @Test
    @DisplayName("Obtiene una métrica que pertenece al cuaderno del negocio")
    void obtieneUnaMetricaDelCuadernoDelNegocio() {
        dadaUnaMetricaDelCuaderno();

        cuandoSeObtiene();

        entoncesSeEncuentraLaMetrica();
    }

    @Test
    @DisplayName("Una métrica inexistente o ajena produce el mismo 404")
    void unaMetricaInexistenteOAjenaDaNoEncontrada() {
        dadoQueElCuadernoNoTieneEsaMetrica();

        cuandoSeIntentaObtener();

        entoncesNoSeEncuentra();
    }

    private void dadaUnaMetricaDelCuaderno() {
        when(metricaRepository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(metrica(lasVentasDelMes(), 0)));
    }

    private void dadoQueElCuadernoNoTieneEsaMetrica() {
        when(metricaRepository.findByIdAndProyectoIdAndNegocioId(METRICA_ID, PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.empty());
    }

    private void cuandoSeObtiene() {
        encontrada = metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID);
    }

    private void cuandoSeIntentaObtener() {
        rechazo = catchThrowable(this::cuandoSeObtiene);
    }

    private void entoncesSeEncuentraLaMetrica() {
        assertThat(encontrada.getId()).isEqualTo(METRICA_ID);
    }

    private void entoncesNoSeEncuentra() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class).hasMessage("Métrica no encontrada");
    }
}
