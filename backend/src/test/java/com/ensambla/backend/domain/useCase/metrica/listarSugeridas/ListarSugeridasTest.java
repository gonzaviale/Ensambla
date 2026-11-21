package com.ensambla.backend.domain.useCase.metrica.listarSugeridas;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.metrica.AgrupacionMetrica;
import com.ensambla.backend.domain.model.metrica.CalculoMetrica;
import com.ensambla.backend.domain.model.metrica.CatalogoMetricas;
import com.ensambla.backend.domain.model.metrica.DefinicionMetrica;
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
import com.ensambla.backend.domain.useCase.metrica.SugeridaItem;
import com.ensambla.backend.infrastructure.config.useCase.MetricaUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
import static com.ensambla.backend.support.MetricaTestData.venta;
import static com.ensambla.backend.support.MetricasDePrueba.unaReceta;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarSugeridasTest {

    private static final DefinicionMetrica LAS_VENTAS_DE_HOY = definicion(
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);

    private static final DefinicionMetrica CUANTAS_VENTAS_HOY = definicion(
            FuenteMetrica.VENTAS, CalculoMetrica.CONTEO, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);

    private static final DefinicionMetrica MIS_CUENTAS_DE_HOY = unaReceta("Mis cuentas", FuenteMetrica.VENTAS,
            CalculoMetrica.CONTEO, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false);

    private static final CatalogoMetricas CATALOGO = new CatalogoMetricas(
            List.of(LAS_VENTAS_DE_HOY), Map.of(RubroNegocio.KIOSCO, List.of(CUANTAS_VENTAS_HOY)));

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final MetricaRepository metricaRepository = mock(MetricaRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final Proyecto proyecto = proyecto();
    private final ListarSugeridas listarSugeridas = new MetricaUseCaseConfig().listarSugeridas(
            negocioRepository,
            proyectoRepository,
            metricaRepository,
            CATALOGO,
            datosDeProyectoService,
            new MotorMetricas(),
            Clock.fixed(AHORA.toInstant(), AHORA.getOffset()));

    private List<SugeridaItem> sugeridas;

    private Throwable rechazo;

    @Test
    @DisplayName("Sugiere las de todos y las del rubro, cada una con lo que daría hoy")
    void sugiereLasDeTodosYLasDelRubroConLoQueDariaHoy() {
        dadoUnNegocioQueHoyVendioPorTrescientos(RubroNegocio.KIOSCO);
        dadoUnPanelCon();

        cuandoSeListanLasSugeridas();

        entoncesSeSugierenEsteOrden(LAS_VENTAS_DE_HOY, CUANTAS_VENTAS_HOY);
        entoncesDarianHoy("300", "1");
    }

    @Test
    @DisplayName("Marca «ya la tenés» la que muestra el panel de ventas del inicio, aunque no esté guardada")
    void marcaYaLaTenesLaDelPanelDeVentas() {
        dadoUnNegocioQueHoyVendioPorTrescientos(RubroNegocio.KIOSCO);
        dadoUnPanelCon();

        cuandoSeListanLasSugeridas();

        entoncesYaLasTiene(true, false);
    }

    @Test
    @DisplayName("Marca «ya la tenés» lo que el cuaderno ya guardó, aunque tenga otro nombre")
    void marcaYaLaTenesLoQueYaSeGuardo() {
        dadoUnNegocioQueHoyVendioPorTrescientos(RubroNegocio.KIOSCO);
        dadoUnPanelCon(metrica(MIS_CUENTAS_DE_HOY, 0));

        cuandoSeListanLasSugeridas();

        entoncesYaLasTiene(true, true);
    }

    @Test
    @DisplayName("Un rubro sin métricas propias recibe sólo las de todos")
    void unRubroSinMetricasPropiasRecibeSoloLasDeTodos() {
        dadoUnNegocioQueHoyVendioPorTrescientos(RubroNegocio.OTRO);
        dadoUnPanelCon();

        cuandoSeListanLasSugeridas();

        entoncesSeSugierenEsteOrden(LAS_VENTAS_DE_HOY);
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se lee nada más")
    void unCuadernoAjenoRespondeNoEncontradoYNoSeLeeNadaMas() {
        dadoUnCuadernoAjeno();

        cuandoSeIntentanListarLasSugeridas();

        entoncesNoSeEncuentraElCuaderno();
        entoncesNoSeLeyeronNiLasMetricasNiLosDatos();
    }

    private void dadoUnNegocioQueHoyVendioPorTrescientos(RubroNegocio rubro) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(rubro));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID))
                .thenReturn(unAlmacenQueVendio(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false)));
    }

    private void dadoUnCuadernoAjeno() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.KIOSCO));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    private void dadoUnPanelCon(Metrica... guardadas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(guardadas));
    }

    private void cuandoSeListanLasSugeridas() {
        sugeridas = listarSugeridas.execute(new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentanListarLasSugeridas() {
        rechazo = catchThrowable(this::cuandoSeListanLasSugeridas);
    }

    private void entoncesSeSugierenEsteOrden(DefinicionMetrica... recetas) {
        assertThat(sugeridas).extracting(sugerida -> sugerida.definicion().calculo())
                .containsExactly(Stream.of(recetas).map(DefinicionMetrica::calculo)
                        .toArray(CalculoMetrica[]::new));
    }

    private void entoncesYaLasTiene(Boolean... yaLasTiene) {
        assertThat(sugeridas).extracting(SugeridaItem::yaLaTenes).containsExactly(yaLasTiene);
    }

    private void entoncesDarianHoy(String... valores) {
        assertThat(sugeridas).extracting(sugerida -> sugerida.resultado().valor())
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(Stream.of(valores).map(BigDecimal::new).toArray(BigDecimal[]::new));
    }

    private void entoncesNoSeEncuentraElCuaderno() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }

    private void entoncesNoSeLeyeronNiLasMetricasNiLosDatos() {
        verifyNoInteractions(metricaRepository, datosDeProyectoService);
    }
}
