package com.ensambla.backend.domain.useCase.metrica.cambiarGrafico;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.GraficoMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasPorCliente;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CambiarGraficoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final CambiarGrafico cambiarGrafico = new MetricaUseCaseConfig().cambiarGrafico(
            negocioRepository, metricaRepository, new TransaccionDirecta());

    private Throwable rechazo;

    @Test
    @DisplayName("Guarda la métrica del cuaderno con el gráfico elegido y la misma receta")
    void guardaLaMetricaConElGraficoElegidoYLaMismaReceta() {
        dadasLasVentasPorClienteEnElCuaderno();

        cuandoSeEligeVerlaComo(GraficoMetrica.DONA);

        entoncesSeGuardoComo(GraficoMetrica.DONA);
    }

    @Test
    @DisplayName("Un gráfico que no sirve para su reparto se rechaza y no se guarda nada")
    void unGraficoQueNoSirveSeRechazaYNoSeGuardaNada() {
        dadasLasVentasPorClienteEnElCuaderno();

        cuandoSeIntentaVerlaComo(GraficoMetrica.LINEA);

        entoncesSeRechazaPorque("Ese gráfico no sirve");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("Una métrica ajena o inexistente responde 404 y no se guarda nada")
    void unaMetricaAjenaRespondeNoEncontradaYNoSeGuardaNada() {
        dadaUnaMetricaAjena();

        cuandoSeIntentaVerlaComo(GraficoMetrica.BARRAS);

        entoncesNoSeEncuentraLaMetrica();
        entoncesNoSeGuardoNada();
    }

    private void dadasLasVentasPorClienteEnElCuaderno() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenReturn(metrica(lasVentasPorCliente(), 0));
    }

    private void dadaUnaMetricaAjena() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenThrow(new ResourceNotFoundException("Métrica no encontrada"));
    }

    private void cuandoSeEligeVerlaComo(GraficoMetrica grafico) {
        cambiarGrafico.execute(new CambiarGraficoRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID, grafico));
    }

    private void cuandoSeIntentaVerlaComo(GraficoMetrica grafico) {
        rechazo = catchThrowable(() -> cuandoSeEligeVerlaComo(grafico));
    }

    private void entoncesSeGuardoComo(GraficoMetrica grafico) {
        ArgumentCaptor<Metrica> captor = ArgumentCaptor.forClass(Metrica.class);
        verify(metricaRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(METRICA_ID);
        assertThat(captor.getValue().getGrafico()).isEqualTo(grafico);
        assertThat(captor.getValue().getDefinicion()).isEqualTo(lasVentasPorCliente());
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }

    private void entoncesNoSeEncuentraLaMetrica() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardoNada() {
        verify(metricaRepository, never()).save(any());
    }
}
