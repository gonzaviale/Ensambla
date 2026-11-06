package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerMetricas.ObtenerMetricasResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerResumenInicio.ObtenerResumenInicioResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");

    private final ReporteUseCaseConfig config = new ReporteUseCaseConfig();

    private NegocioRepository negocioRepository;
    private BloqueRepository bloqueRepository;
    private CampoRepository campoRepository;
    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        negocioRepository = mock(NegocioRepository.class);
        bloqueRepository = mock(BloqueRepository.class);
        campoRepository = mock(CampoRepository.class);
        reporteService = mock(ReporteService.class);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Bloque.builder().id(BLOQUE_ID).nombre("Pedidos").build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(reporteService.generar(any(), any(), anyInt()))
                .thenReturn(new ReporteRegistros(List.of(), new TotalesRegistros(0, Map.of()), false));
    }

    @Test
    @DisplayName("El wiring de generar conecta los servicios y el mapper")
    void generarReporteUseCaseShouldWireCollaborators() {
        GenerarReporteResponse response = config.generarReporte(
                        negocioRepository, mock(ControlPlanService.class), Clock.systemUTC(),
                        bloqueRepository, campoRepository, reporteService)
                .execute(new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));

        assertThat(response.filas()).isEmpty();
        assertThat(response.maximoFilas()).isEqualTo(50);
    }

    @Test
    @DisplayName("El wiring de exportar conecta los servicios, el puerto del archivo, el mapper y el reloj")
    void exportarReporteUseCaseShouldWireCollaborators() {
        ExportadorReportePort exportador = tabla -> new byte[] {1, 2};

        ExportarReporteResponse response = config.exportarReporte(
                        negocioRepository, mock(ControlPlanService.class), bloqueRepository, reporteService,
                        mock(RegistroRepository.class), mock(CampoRepository.class), exportador, Clock.systemUTC())
                .execute(new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));

        assertThat(response.contenido()).containsExactly(1, 2);
        assertThat(response.nombreArchivo()).startsWith("reporte-pedidos-");
    }

    @Test
    @DisplayName("El wiring del resumen conecta el servicio de datos, la zona, los avisos y el generador del PDF")
    void exportarResumenNegocioUseCaseShouldWireCollaborators() {
        ProyectoId proyectoId = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        Proyecto proyecto = Proyecto.builder().id(proyectoId).nombre("Almacén").bloques(List.of()).build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(proyectoRepository.obtener(NEGOCIO_ID, proyectoId)).thenReturn(proyecto);
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
        AvisoRepository avisoRepository = mock(AvisoRepository.class);
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        GeneradorResumenPdfPort generador = resumen -> new byte[] {7};

        ExportarResumenResponse response = config.exportarResumenNegocio(
                        negocioRepository, mock(ControlPlanService.class), proyectoRepository,
                        config.datosDeProyectoService(mock(RegistroRepository.class)), mock(ZonaRepository.class),
                        mock(AgregadoZonaRepository.class), avisoRepository, generador, Clock.systemUTC())
                .execute(new ExportarResumenRequest(USUARIO_ID, proyectoId, null, null));

        assertThat(response.contenido()).containsExactly(7);
        assertThat(response.nombreArchivo()).startsWith("resumen-almacen-");
    }

    @Test
    @DisplayName("El wiring de las métricas del inicio conecta el servicio de datos y el reloj")
    void obtenerResumenInicioUseCaseShouldWireCollaborators() {
        ProyectoId proyectoId = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        Proyecto proyecto = Proyecto.builder().id(proyectoId).nombre("Almacén").bloques(List.of()).build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(proyectoRepository.obtener(NEGOCIO_ID, proyectoId)).thenReturn(proyecto);

        ObtenerResumenInicioResponse response = config.obtenerResumenInicio(
                        negocioRepository, proyectoRepository,
                        config.datosDeProyectoService(mock(RegistroRepository.class)), Clock.systemUTC())
                .execute(new ObtenerResumenInicioRequest(USUARIO_ID, proyectoId));

        assertThat(response.sistema()).isEqualTo("Almacén");
        assertThat(response.hayVentas()).isFalse();
    }

    @Test
    @DisplayName("El wiring de las métricas detalladas conecta el servicio de datos y el reloj")
    void obtenerMetricasUseCaseShouldWireCollaborators() {
        ProyectoId proyectoId = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        Proyecto proyecto = Proyecto.builder().id(proyectoId).nombre("Almacén").bloques(List.of()).build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(proyectoRepository.obtener(NEGOCIO_ID, proyectoId)).thenReturn(proyecto);

        ObtenerMetricasResponse response = config.obtenerMetricas(
                        negocioRepository, proyectoRepository,
                        config.datosDeProyectoService(mock(RegistroRepository.class)), Clock.systemUTC())
                .execute(new ObtenerMetricasRequest(USUARIO_ID, proyectoId, PeriodoMetricas.MES));

        assertThat(response.sistema()).isEqualTo("Almacén");
        assertThat(response.porDia()).hasSize(30);
    }
}
