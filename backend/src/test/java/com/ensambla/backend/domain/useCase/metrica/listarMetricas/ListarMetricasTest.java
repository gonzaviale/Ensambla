package com.ensambla.backend.domain.useCase.metrica.listarMetricas;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MotorMetricas;
import com.ensambla.backend.domain.model.metrica.PeriodoMetrica;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.metrica.MetricaRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.metrica.MetricaItem;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import org.junit.jupiter.api.BeforeEach;
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
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.inventario;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricaTestData.ventas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarMetricasTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final ListarMetricas listarMetricas = new MetricaUseCaseConfig().listarMetricas(
            negocioRepository,
            proyectoRepository,
            metricaRepository,
            datosDeProyectoService,
            new MotorMetricas(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
    }

    @Test
    @DisplayName("Lista las métricas del cuaderno en su orden, cada una con lo que da hoy")
    void executeShouldListMetricsWithTheirResults() {
        Metrica ventasDeHoy = metrica(
                definicion(FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY),
                0);
        Metrica gastos = metrica(
                definicion(FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES),
                1);
        givenMetricas(ventasDeHoy, gastos);
        givenDatos();

        List<MetricaItem> result = whenListar();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(ventasDeHoy.getId());
        assertThat(result.get(0).orden()).isZero();
        assertThat(result.get(0).resultado().valor()).isEqualByComparingTo("300");
        assertThat(result.get(1).resultado().valor()).isNull();
        assertThat(result.get(1).resultado().motivo()).contains("tabla de gastos");
    }

    @Test
    @DisplayName("Un cuaderno sin métricas recibe una lista vacía")
    void executeShouldReturnEmptyListWhenThereAreNoMetrics() {
        givenMetricas();
        givenDatos();

        List<MetricaItem> result = whenListar();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se lee nada más")
    void executeShouldPropagateNotFoundForForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(this::whenListar).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(metricaRepository, datosDeProyectoService);
    }

    // --- arrange ---
    private void givenMetricas(Metrica... metricas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(metricas));
    }

    private void givenDatos() {
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(datos(
                List.of(inventario()),
                List.of(ventas(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false))),
                List.of()));
    }

    // --- act ---
    private List<MetricaItem> whenListar() {
        return listarMetricas.execute(new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
    }
}
