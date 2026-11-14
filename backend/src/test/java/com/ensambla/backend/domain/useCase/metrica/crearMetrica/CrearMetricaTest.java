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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.util.ArrayList;
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

    @BeforeEach
    void setUp() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio(RubroNegocio.ALMACEN));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(datos(
                List.of(inventario()),
                List.of(ventas(venta(PAN_ID, 2, 150, "2026-11-11", "Ana", false))),
                List.of()));
        when(metricaRepository.save(any())).thenAnswer(invocacion -> {
            Metrica guardada = invocacion.getArgument(0);
            guardada.setId(NUEVA_ID);
            return guardada;
        });
    }

    @Test
    @DisplayName("Guarda la métrica al final del panel y devuelve lo que da hoy")
    void executeShouldSaveAtTheEndOfThePanel() {
        givenGuardadas(metrica(otraReceta(CalculoMetrica.CONTEO), 0), metrica(otraReceta(CalculoMetrica.PROMEDIO), 2));

        MetricaItem result = crearMetrica.execute(request(CalculoMetrica.SUMA, AutoriaMetrica.SUGERIDA));

        assertThat(result.id()).isEqualTo(NUEVA_ID);
        assertThat(result.orden()).isEqualTo(3);
        assertThat(result.autoria()).isEqualTo(AutoriaMetrica.SUGERIDA);
        assertThat(result.definicion().nombre()).isEqualTo("Ventas de hoy");
        assertThat(result.resultado().valor()).isEqualByComparingTo("300");
        Metrica guardada = capturarGuardada();
        assertThat(guardada.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(guardada.getProyectoId()).isEqualTo(PROYECTO_ID);
        assertThat(guardada.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("La primera métrica del cuaderno queda en el lugar cero")
    void executeShouldStartAtPlaceZero() {
        givenGuardadas();

        MetricaItem result = crearMetrica.execute(request(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA));

        assertThat(result.orden()).isZero();
        assertThat(result.autoria()).isEqualTo(AutoriaMetrica.PROPIA);
    }

    @Test
    @DisplayName("No guarda dos veces la misma receta aunque se llame distinto")
    void executeShouldRejectDuplicateRecipe() {
        givenGuardadas(metrica(otraReceta(CalculoMetrica.SUMA), 0));

        assertThatThrownBy(() -> crearMetrica.execute(request(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ya tenés esa métrica");
        verify(metricaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No guarda más del máximo de métricas por cuaderno")
    void executeShouldRejectWhenThePanelIsFull() {
        List<Metrica> llenas = new ArrayList<>();
        for (int lugar = 0; lugar < Metrica.MAXIMO_POR_CUADERNO; lugar++) {
            llenas.add(metrica(otraReceta(CalculoMetrica.CONTEO), lugar));
        }
        givenGuardadas(llenas.toArray(Metrica[]::new));

        assertThatThrownBy(() -> crearMetrica.execute(request(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quitá alguna");
        verify(metricaRepository, never()).save(any());
    }

    @Test
    @DisplayName("No guarda una receta inválida")
    void executeShouldRejectInvalidRecipe() {
        givenGuardadas();
        CrearMetricaRequest invalida = new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Mal", FuenteMetrica.GASTOS,
                CalculoMetrica.UNIDADES, null, null, false, AutoriaMetrica.PROPIA);

        assertThatThrownBy(() -> crearMetrica.execute(invalida)).isInstanceOf(BusinessRuleException.class);
        verify(metricaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un cuaderno ajeno o inexistente responde 404 y no se guarda nada")
    void executeShouldPropagateNotFoundForForeignProject() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> crearMetrica.execute(request(CalculoMetrica.SUMA, AutoriaMetrica.PROPIA)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(metricaRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenGuardadas(Metrica... metricas) {
        when(metricaRepository.findAllByProyectoIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(List.of(metricas));
    }

    // --- assert ---
    private Metrica capturarGuardada() {
        ArgumentCaptor<Metrica> captor = ArgumentCaptor.forClass(Metrica.class);
        verify(metricaRepository).save(captor.capture());
        return captor.getValue();
    }

    // --- helpers ---
    private static CrearMetricaRequest request(CalculoMetrica calculo, AutoriaMetrica autoria) {
        return new CrearMetricaRequest(USUARIO_ID, PROYECTO_ID, "Ventas de hoy", FuenteMetrica.VENTAS, calculo,
                AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY, false, autoria);
    }

    private static DefinicionMetrica otraReceta(CalculoMetrica calculo) {
        return definicion(FuenteMetrica.VENTAS, calculo, AgrupacionMetrica.NINGUNA, PeriodoMetrica.HOY);
    }
}
