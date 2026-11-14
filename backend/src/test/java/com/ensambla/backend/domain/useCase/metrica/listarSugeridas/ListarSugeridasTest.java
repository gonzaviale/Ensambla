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

import java.time.Clock;
import java.util.List;
import java.util.Map;

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

class ListarSugeridasTest {

    private static final DefinicionMetrica BASE = definicion(
            FuenteMetrica.VENTAS, CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);
    private static final DefinicionMetrica DEL_KIOSCO = definicion(
            FuenteMetrica.VENTAS, CalculoMetrica.CONTEO, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);
    private static final CatalogoMetricas CATALOGO =
            new CatalogoMetricas(List.of(BASE), Map.of(RubroNegocio.KIOSCO, List.of(DEL_KIOSCO)));

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

    @Test
    @DisplayName("Sugiere las de todos y las del rubro, cada una con lo que daría hoy")
    void executeShouldSuggestBaseAndRubroMetricsWithPreview() {
        givenNegocioDeRubro(RubroNegocio.KIOSCO);
        givenMetricasGuardadas();

        List<SugeridaItem> result = whenListar();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).definicion().calculo()).isEqualTo(CalculoMetrica.SUMA);
        assertThat(result.get(0).resultado().valor()).isEqualByComparingTo("300");
        assertThat(result.get(1).definicion().calculo()).isEqualTo(CalculoMetrica.CONTEO);
        assertThat(result.get(1).resultado().valor()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("No vuelve a sugerir lo que el cuaderno ya guardó, aunque tenga otro nombre")
    void executeShouldSkipMetricsAlreadySaved() {
        givenNegocioDeRubro(RubroNegocio.KIOSCO);
        DefinicionMetrica conOtroNombre = new DefinicionMetrica("Mis ventas", FuenteMetrica.VENTAS,
                CalculoMetrica.SUMA, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false);
        givenMetricasGuardadas(metrica(conOtroNombre, 0));

        List<SugeridaItem> result = whenListar();

        assertThat(result).singleElement().satisfies(
                sugerida -> assertThat(sugerida.definicion().calculo()).isEqualTo(CalculoMetrica.CONTEO));
    }

    @Test
    @DisplayName("Un rubro sin métricas propias recibe sólo las de todos")
    void executeShouldSuggestOnlyBaseForRubroWithoutOwnMetrics() {
        givenNegocioDeRubro(RubroNegocio.OTRO);
        givenMetricasGuardadas();

        List<SugeridaItem> result = whenListar();

        assertThat(result).singleElement().satisfies(
                sugerida -> assertThat(sugerida.definicion().calculo()).isEqualTo(CalculoMetrica.SUMA));
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se lee nada más")
    void executeShouldPropagateNotFoundForForeignProject() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.KIOSCO));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(this::whenListar).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(metricaRepository, datosDeProyectoService);
    }

    // --- arrange ---
    private void givenNegocioDeRubro(RubroNegocio rubro) {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(rubro));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(datos(
                List.of(inventario()),
                List.of(ventas(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false))),
                List.of()));
    }

    private void givenMetricasGuardadas(Metrica... metricas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(metricas));
    }

    // --- act ---
    private List<SugeridaItem> whenListar() {
        return listarSugeridas.execute(new ListarSugeridasRequest(USUARIO_ID, PROYECTO_ID));
    }
}
