package com.ensambla.backend.domain.useCase.metrica.previsualizarMetrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.ResultadoItem;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PrevisualizarMetricaTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final PrevisualizarMetrica previsualizarMetrica = new MetricaUseCaseConfig().previsualizarMetrica(
            negocioRepository,
            proyectoRepository,
            datosDeProyectoService,
            new MotorMetricas(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private ResultadoItem resultado;

    private Throwable rechazo;

    @Test
    @DisplayName("Calcula la receta con los datos del cuaderno sin guardar nada")
    void calculaLaRecetaConLosDatosDelCuaderno() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();

        cuandoSePrevisualiza(lasVentasDeHoyPedidas());

        entoncesDaConFilasUsadas("300", 1);
    }

    @Test
    @DisplayName("Rechaza una receta que no se puede calcular antes de leer el cuaderno")
    void rechazaUnaRecetaInvalidaAntesDeLeerElCuaderno() {
        cuandoSeIntentaPrevisualizar(lasUnidadesDeLosGastosPedidas());

        entoncesSeRechaza();
        entoncesNoSeLeyoElCuaderno();
    }

    private static PrevisualizarMetricaRequest lasVentasDeHoyPedidas() {
        return new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas de hoy", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false);
    }

    private static PrevisualizarMetricaRequest lasUnidadesDeLosGastosPedidas() {
        return new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID, "Mal", FuenteMetrica.GASTOS,
                CalculoMetrica.UNIDADES, null, null, false);
    }

    private void dadoUnAlmacenQueHoyVendioPorTrescientos() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(unAlmacenQueVendio(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false)));
    }

    private void cuandoSePrevisualiza(PrevisualizarMetricaRequest pedido) {
        resultado = previsualizarMetrica.execute(pedido);
    }

    private void cuandoSeIntentaPrevisualizar(PrevisualizarMetricaRequest pedido) {
        rechazo = catchThrowable(() -> cuandoSePrevisualiza(pedido));
    }

    private void entoncesDaConFilasUsadas(String valor, long filasUsadas) {
        assertThat(resultado.valor()).isEqualByComparingTo(valor);
        assertThat(resultado.deDondeSale().filasUsadas()).isEqualTo(filasUsadas);
    }

    private void entoncesSeRechaza() {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class);
    }

    private void entoncesNoSeLeyoElCuaderno() {
        verifyNoInteractions(negocioRepository, proyectoRepository, datosDeProyectoService);
    }
}
