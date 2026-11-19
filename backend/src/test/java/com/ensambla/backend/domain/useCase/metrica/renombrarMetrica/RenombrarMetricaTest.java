package com.ensambla.backend.domain.useCase.metrica.renombrarMetrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
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
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMes;
import static com.ensambla.backend.support.MetricasDePrueba.lasVentasDelMesLlamadas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RenombrarMetricaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final RenombrarMetrica renombrarMetrica = new MetricaUseCaseConfig().renombrarMetrica(
            negocioRepository, metricaRepository, new TransaccionDirecta());

    private Throwable rechazo;

    @Test
    @DisplayName("Guarda la métrica del cuaderno con el nombre nuevo y la misma receta")
    void guardaLaMetricaConElNombreNuevoYLaMismaReceta() {
        dadaLasVentasDelMesEnElCuaderno();

        cuandoSeRenombraA("Lo que vendí");

        entoncesSeGuardoLlamada("Lo que vendí");
    }

    @Test
    @DisplayName("Un nombre vacío se rechaza y no se guarda nada")
    void unNombreVacioSeRechazaYNoSeGuardaNada() {
        dadaLasVentasDelMesEnElCuaderno();

        cuandoSeIntentaRenombrarA("   ");

        entoncesSeRechazaPorque("nombre");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("Una métrica ajena o inexistente responde 404 y no se guarda nada")
    void unaMetricaAjenaRespondeNoEncontradaYNoSeGuardaNada() {
        dadaUnaMetricaAjena();

        cuandoSeIntentaRenombrarA("Lo que vendí");

        entoncesNoSeEncuentraLaMetrica();
        entoncesNoSeGuardoNada();
    }

    private void dadaLasVentasDelMesEnElCuaderno() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenReturn(metrica(lasVentasDelMes(), 0));
    }

    private void dadaUnaMetricaAjena() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_ID))
                .thenThrow(new ResourceNotFoundException("Métrica no encontrada"));
    }

    private void cuandoSeRenombraA(String nombre) {
        renombrarMetrica.execute(new RenombrarMetricaRequest(USUARIO_ID, PROYECTO_ID, METRICA_ID, nombre));
    }

    private void cuandoSeIntentaRenombrarA(String nombre) {
        rechazo = catchThrowable(() -> cuandoSeRenombraA(nombre));
    }

    private void entoncesSeGuardoLlamada(String nombre) {
        ArgumentCaptor<Metrica> captor = ArgumentCaptor.forClass(Metrica.class);
        verify(metricaRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(METRICA_ID);
        assertThat(captor.getValue().getDefinicion()).isEqualTo(lasVentasDelMesLlamadas(nombre));
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
