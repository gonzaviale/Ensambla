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
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.datos;
import static com.ensambla.backend.support.MetricaTestData.inventario;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricaTestData.ventas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    @DisplayName("Calcula la receta con los datos del cuaderno sin guardar nada")
    void executeShouldCalculateTheRecipe() {
        givenCuaderno();

        ResultadoItem result = previsualizarMetrica.execute(request(CalculoMetrica.SUMA));

        assertThat(result.valor()).isEqualByComparingTo("300");
        assertThat(result.deDondeSale().filasUsadas()).isEqualTo(1);
    }

    @Test
    @DisplayName("Rechaza una receta que no se puede calcular antes de leer el cuaderno")
    void executeShouldRejectInvalidRecipe() {
        assertThatThrownBy(() -> previsualizarMetrica.execute(new PrevisualizarMetricaRequest(
                USUARIO_ID, PROYECTO_ID, "Mal", FuenteMetrica.GASTOS, CalculoMetrica.UNIDADES, null, null, false)))
                .isInstanceOf(BusinessRuleException.class);
        verifyNoInteractions(negocioRepository, proyectoRepository, datosDeProyectoService);
    }

    // --- arrange ---
    private void givenCuaderno() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(datos(
                List.of(inventario()),
                List.of(ventas(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false))),
                List.of()));
    }

    // --- helpers ---
    private static PrevisualizarMetricaRequest request(CalculoMetrica calculo) {
        return new PrevisualizarMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas de hoy", FuenteMetrica.VENTAS,
                calculo, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false);
    }
}
