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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.METRICA_ID;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarMetricasTest {

    private static final Metrica VENTAS_DE_HOY = metrica(
            definicion(FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY), 0);

    private static final Metrica GASTOS_DEL_MES = metrica(
            definicion(FuenteMetrica.GASTOS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.MES), 1);

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

    private List<MetricaItem> metricas;

    private Throwable rechazo;

    @Test
    @DisplayName("Lista las métricas del cuaderno en su orden, cada una con lo que da hoy")
    void listaLasMetricasEnSuOrdenConLoQueDaHoy() {
        dadoUnAlmacenQueHoyVendioPorTrescientosYNoTieneGastos();
        dadoUnPanelCon(VENTAS_DE_HOY, GASTOS_DEL_MES);

        cuandoSeListan();

        entoncesLaPrimeraEsLaDeVentasYDa("300");
        entoncesLaSegundaNoSePuedeCalcularPorque("Todavía no cargaste gastos");
    }

    @Test
    @DisplayName("Un cuaderno sin métricas recibe una lista vacía")
    void unCuadernoSinMetricasRecibeUnaListaVacia() {
        dadoUnAlmacenQueHoyVendioPorTrescientosYNoTieneGastos();
        dadoUnPanelCon();

        cuandoSeListan();

        entoncesNoHayMetricas();
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se lee nada más")
    void unCuadernoAjenoRespondeNoEncontradoYNoSeLeeNadaMas() {
        dadoUnCuadernoAjeno();

        cuandoSeIntentanListar();

        entoncesNoSeEncuentraElCuaderno();
        entoncesNoSeLeyeronNiLasMetricasNiLosDatos();
    }

    private void dadoUnAlmacenQueHoyVendioPorTrescientosYNoTieneGastos() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(unAlmacenQueVendio(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false)));
    }

    private void dadoUnCuadernoAjeno() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void dadoUnPanelCon(Metrica... guardadas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(guardadas));
    }

    private void cuandoSeListan() {
        metricas = listarMetricas.execute(new ListarMetricasRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentanListar() {
        rechazo = catchThrowable(this::cuandoSeListan);
    }

    private void entoncesLaPrimeraEsLaDeVentasYDa(String valor) {
        assertThat(metricas).hasSize(2);
        assertThat(metricas.getFirst().id()).isEqualTo(METRICA_ID);
        assertThat(metricas.getFirst().orden()).isZero();
        assertThat(metricas.getFirst().resultado().valor()).isEqualByComparingTo(valor);
    }

    private void entoncesLaSegundaNoSePuedeCalcularPorque(String motivo) {
        assertThat(metricas.get(1).resultado().valor()).isNull();
        assertThat(metricas.get(1).resultado().motivo()).contains(motivo);
    }

    private void entoncesNoHayMetricas() {
        assertThat(metricas).isEmpty();
    }

    private void entoncesNoSeEncuentraElCuaderno() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeLeyeronNiLasMetricasNiLosDatos() {
        verifyNoInteractions(metricaRepository, datosDeProyectoService);
    }
}
