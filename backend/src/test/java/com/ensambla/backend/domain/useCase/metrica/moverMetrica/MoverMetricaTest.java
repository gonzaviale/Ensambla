package com.ensambla.backend.domain.useCase.metrica.moverMetrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.METRICA_GASTOS_ID;
import static com.ensambla.backend.support.MetricaTestData.METRICA_STOCK_ID;
import static com.ensambla.backend.support.MetricaTestData.METRICA_VENTAS_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.unPanelDeVentasGastosYStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MoverMetricaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final MoverMetrica moverMetrica = new MetricaUseCaseConfig().moverMetrica(
            negocioRepository, metricaRepository, new TransaccionDirecta());

    private Throwable rechazo;

    @Test
    @DisplayName("Sube el stock al primer lugar y guarda el panel entero renumerado")
    void subeElStockAlPrimerLugarYGuardaElPanelRenumerado() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeMueve(METRICA_STOCK_ID, 0);

        entoncesSeGuardoElPanelEnEsteOrden(METRICA_STOCK_ID, METRICA_VENTAS_ID, METRICA_GASTOS_ID);
    }

    @Test
    @DisplayName("Un lugar fuera del panel se rechaza y no se guarda nada")
    void unLugarFueraDelPanelSeRechazaYNoSeGuardaNada() {
        dadoUnPanelDeVentasGastosYStock();

        cuandoSeIntentaMover(METRICA_VENTAS_ID, 5);

        entoncesSeRechazaPorque("Ese lugar no está en el panel");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("Una métrica ajena o inexistente responde 404 y no se guarda nada")
    void unaMetricaAjenaRespondeNoEncontradaYNoSeGuardaNada() {
        dadaUnaMetricaAjena();

        cuandoSeIntentaMover(METRICA_VENTAS_ID, 0);

        entoncesNoSeEncuentraLaMetrica();
        entoncesNoSeGuardoNada();
    }

    private void dadoUnPanelDeVentasGastosYStock() {
        List<Metrica> panel = unPanelDeVentasGastosYStock();
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        panel.forEach(metrica -> when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, metrica.getId()))
                .thenReturn(metrica));
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(panel);
    }

    private void dadaUnaMetricaAjena() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(metricaRepository.obtener(NEGOCIO_ID, PROYECTO_ID, METRICA_VENTAS_ID))
                .thenThrow(new ResourceNotFoundException("Métrica no encontrada"));
    }

    private void cuandoSeMueve(MetricaId metricaId, int lugar) {
        moverMetrica.execute(new MoverMetricaRequest(USUARIO_ID, PROYECTO_ID, metricaId, lugar));
    }

    private void cuandoSeIntentaMover(MetricaId metricaId, int lugar) {
        rechazo = catchThrowable(() -> cuandoSeMueve(metricaId, lugar));
    }

    private void entoncesSeGuardoElPanelEnEsteOrden(MetricaId... ids) {
        ArgumentCaptor<Metrica> captor = ArgumentCaptor.forClass(Metrica.class);
        verify(metricaRepository, times(ids.length)).save(captor.capture());
        assertThat(captor.getAllValues()).extracting(Metrica::getId).containsExactly(ids);
        assertThat(captor.getAllValues()).extracting(Metrica::getOrden).containsExactly(0, 1, 2);
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
