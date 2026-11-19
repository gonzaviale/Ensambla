package com.ensambla.backend.domain.useCase.metrica.crearMetrica;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.AutoriaMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
import com.ensambla.backend.domain.model.metrica.FuenteMetrica;
import com.ensambla.backend.domain.model.metrica.Metrica;
import com.ensambla.backend.domain.model.metrica.MetricaId;
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
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.List;

import static com.ensambla.backend.support.MetricaTestData.AHORA;
import static com.ensambla.backend.support.MetricaTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.MetricaTestData.PAN_ID;
import static com.ensambla.backend.support.MetricaTestData.PROYECTO_ID;
import static com.ensambla.backend.support.MetricaTestData.USUARIO_ID;
import static com.ensambla.backend.support.MetricaTestData.definicion;
import static com.ensambla.backend.support.MetricaTestData.metrica;
import static com.ensambla.backend.support.MetricaTestData.negocio;
import static com.ensambla.backend.support.MetricaTestData.proyecto;
import static com.ensambla.backend.support.MetricaTestData.unAlmacenQueVendio;
import static com.ensambla.backend.support.MetricaTestData.unPanelLleno;
import static com.ensambla.backend.support.MetricaTestData.venta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrearMetricaTest {

    private static final MetricaId NUEVA_ID = MetricaId.de("e0000000-0000-4000-8000-000000000099");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final CrearMetrica crearMetrica = new MetricaUseCaseConfig().crearMetrica(
            negocioRepository,
            proyectoRepository,
            metricaRepository,
            datosDeProyectoService,
            new MotorMetricas(),
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private MetricaItem creada;

    private Throwable rechazo;

    @Test
    @DisplayName("Guarda la métrica al final del panel y devuelve lo que da hoy")
    void guardaLaMetricaAlFinalDelPanelYDevuelveLoQueDaHoy() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();
        dadoUnPanelCon(metrica(lasVentasDeHoy(CalculoMetrica.CONTEO), 0),
                metrica(lasVentasDeHoy(CalculoMetrica.PROMEDIO), 2));

        cuandoSeAgrega(lasVentasDeHoyPedidas(CalculoMetrica.SUMA, AutoriaMetrica.SUGERIDA));

        entoncesQuedoEnElLugar(3, AutoriaMetrica.SUGERIDA);
        entoncesDaHoy("300");
        entoncesSeGuardoEnElCuadernoDelNegocio();
    }

    @Test
    @DisplayName("La primera métrica del cuaderno queda en el lugar cero")
    void laPrimeraMetricaQuedaEnElLugarCero() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();
        dadoUnPanelCon();

        cuandoSeAgrega(lasVentasDeHoyPedidas(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA));

        entoncesQuedoEnElLugar(0, AutoriaMetrica.PROPIA);
    }

    @Test
    @DisplayName("No guarda dos veces la misma receta aunque se llame distinto")
    void noGuardaDosVecesLaMismaReceta() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();
        dadoUnPanelCon(metrica(lasVentasDeHoy(CalculoMetrica.SUMA), 0));

        cuandoSeIntentaAgregar(lasVentasDeHoyPedidas(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA));

        entoncesSeRechazaPorque("Ya tenés esa métrica");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("No guarda más del máximo de métricas por cuaderno")
    void noGuardaMasDelMaximoPorCuaderno() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();
        dadoUnPanelCon(unPanelLleno());

        cuandoSeIntentaAgregar(lasVentasDeHoyPedidas(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA));

        entoncesSeRechazaPorque("quitá alguna");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("No guarda una receta inválida")
    void noGuardaUnaRecetaInvalida() {
        dadoUnAlmacenQueHoyVendioPorTrescientos();
        dadoUnPanelCon();

        cuandoSeIntentaAgregar(lasUnidadesDeLosGastosPedidas());

        entoncesSeRechazaPorque("unidades");
        entoncesNoSeGuardoNada();
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se guarda nada")
    void unCuadernoAjenoRespondeNoEncontradoYNoSeGuardaNada() {
        dadoUnCuadernoAjeno();

        cuandoSeIntentaAgregar(lasVentasDeHoyPedidas(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA));

        entoncesNoSeEncuentraElCuaderno();
        entoncesNoSeGuardoNada();
    }

    private static DefinicionMetrica lasVentasDeHoy(CalculoMetrica calculo) {
        return definicion(FuenteMetrica.VENTAS, calculo, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);
    }

    private static CrearMetricaRequest lasVentasDeHoyPedidas(CalculoMetrica calculo, AutoriaMetrica autoria) {
        return new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas de hoy", FuenteMetrica.VENTAS, calculo,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false, autoria);
    }

    private static CrearMetricaRequest lasUnidadesDeLosGastosPedidas() {
        return new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Mal", FuenteMetrica.GASTOS, CalculoMetrica.UNIDADES,
                null, null, false, AutoriaMetrica.PROPIA);
    }

    private void dadoUnAlmacenQueHoyVendioPorTrescientos() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(unAlmacenQueVendio(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false)));
        when(metricaRepository.save(any())).thenAnswer(invocacion -> conIdNuevo(invocacion.getArgument(0)));
    }

    private void dadoUnCuadernoAjeno() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void dadoUnPanelCon(Metrica... metricas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(metricas));
    }

    private static Metrica conIdNuevo(Metrica guardada) {
        guardada.setId(NUEVA_ID);
        return guardada;
    }

    private void cuandoSeAgrega(CrearMetricaRequest pedido) {
        creada = crearMetrica.execute(pedido);
    }

    private void cuandoSeIntentaAgregar(CrearMetricaRequest pedido) {
        rechazo = catchThrowable(() -> cuandoSeAgrega(pedido));
    }

    private void entoncesQuedoEnElLugar(int orden, AutoriaMetrica autoria) {
        assertThat(creada.id()).isEqualTo(NUEVA_ID);
        assertThat(creada.orden()).isEqualTo(orden);
        assertThat(creada.autoria()).isEqualTo(autoria);
        assertThat(creada.definicion().nombre()).isEqualTo("Ventas de hoy");
    }

    private void entoncesDaHoy(String valor) {
        assertThat(creada.resultado().valor()).isEqualByComparingTo(valor);
    }

    private void entoncesSeGuardoEnElCuadernoDelNegocio() {
        ArgumentCaptor<Metrica> captor = ArgumentCaptor.forClass(Metrica.class);
        verify(metricaRepository).save(captor.capture());
        assertThat(captor.getValue().getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(captor.getValue().getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(captor.getValue().getCreadoEn()).isEqualTo(AHORA);
    }

    private void entoncesSeRechazaPorque(String motivo) {
        assertThat(rechazo).isInstanceOf(BusinessRuleException.class).hasMessageContaining(motivo);
    }

    private void entoncesNoSeEncuentraElCuaderno() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeGuardoNada() {
        verify(metricaRepository, never()).save(any());
    }
}
