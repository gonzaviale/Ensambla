package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.PeriodoMetricas;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.caja.CobroCajaRepository;
import com.ensambla.backend.domain.repository.caja.VentaCajaRepository;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoRequest;
import com.ensambla.backend.domain.useCase.reporte.obtenerAvisoEconomico.ObtenerAvisoEconomicoResponse;
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
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");

    private final ReporteUseCaseConfig config = new ReporteUseCaseConfig();
    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final CampoRepository campoRepository = mock(CampoRepository.class);
    private final ReporteService reporteService = mock(ReporteService.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private GenerarReporteResponse reporte;
    private ExportarReporteResponse reporteExportado;
    private ExportarResumenResponse resumenExportado;
    private ObtenerResumenInicioResponse resumenInicio;
    private ObtenerMetricasResponse metricas;
    private ObtenerAvisoEconomicoResponse avisoEconomico;

    @BeforeEach
    void dadoUnBloqueDePedidosSinFilas() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueRepository.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Bloque.builder().id(BLOQUE_ID).nombre("Pedidos").build());
        when(campoRepository.findByBloqueIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(reporteService.generar(any(), any(), anyInt()))
                .thenReturn(new ReporteRegistros(List.of(), new TotalesRegistros(0, Map.of()), false));
    }

    @Test
    @DisplayName("El wiring de generar conecta los servicios y el mapper")
    void elWiringDeGenerarConectaLosServicios() {
        cuandoSeGeneraElReporte();

        entoncesElReporteEstaVacioConMaximoDeFilas(50);
    }

    @Test
    @DisplayName("El wiring de exportar conecta los servicios, el puerto del archivo, el mapper y el reloj")
    void elWiringDeExportarConectaLosServiciosYElArchivo() {
        cuandoSeExportaElReporte();

        entoncesElReporteExportadoSeLlamaYContiene("reporte-pedidos-", 1, 2);
    }

    @Test
    @DisplayName("El wiring del resumen conecta el servicio de datos, la zona, los avisos y el generador del PDF")
    void elWiringDelResumenConectaDatosZonaAvisosYPdf() {
        dadoElProyectoAlmacenSinZonaNiAvisos();

        cuandoSeExportaElResumen();

        entoncesElResumenSeLlamaYContiene("resumen-almacen-", 7);
    }

    @Test
    @DisplayName("El wiring de las métricas del inicio conecta el servicio de datos y el reloj")
    void elWiringDelResumenDeInicioConectaDatosYReloj() {
        dadoElProyectoAlmacen();

        cuandoSeObtieneElResumenDeInicio();

        entoncesElInicioEsDelSistemaSinVentas("Almacén");
    }

    @Test
    @DisplayName("El wiring de las métricas detalladas conecta el servicio de datos, la zona, los avisos y el reloj")
    void elWiringDeLasMetricasConectaDatosZonaAvisosYReloj() {
        dadoElProyectoAlmacen();

        cuandoSeObtienenLasMetricasDelMes();

        entoncesLasMetricasSonDelSistemaConDias("Almacén", 30);
    }

    @Test
    @DisplayName("El wiring del aviso económico conecta el servicio de datos y el reloj")
    void elWiringDelAvisoEconomicoConectaDatosYReloj() {
        dadoElProyectoAlmacen();

        cuandoSeObtieneElAvisoEconomico();

        entoncesNoHayAvisoEconomico();
    }

    private void dadoElProyectoAlmacen() {
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenReturn(Proyecto.builder().id(PROYECTO_ID).nombre("Almacén").bloques(List.of()).build());
    }

    private void dadoElProyectoAlmacenSinZonaNiAvisos() {
        dadoElProyectoAlmacen();
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
    }

    private void cuandoSeGeneraElReporte() {
        reporte = config.generarReporte(negocioRepository, mock(ControlPlanService.class), Clock.systemUTC(),
                bloqueRepository, campoRepository, reporteService).execute(unPedidoDeReporte());
    }

    private void cuandoSeExportaElReporte() {
        ExportadorReportePort exportador = tabla -> new byte[] {1, 2};
        reporteExportado = config.exportarReporte(negocioRepository, mock(ControlPlanService.class), bloqueRepository,
                reporteService, mock(RegistroRepository.class), mock(CampoRepository.class), exportador,
                Clock.systemUTC()).execute(unPedidoDeReporte());
    }

    private void cuandoSeExportaElResumen() {
        GeneradorResumenPdfPort generador = resumen -> new byte[] {7};
        resumenExportado = config.exportarResumenNegocio(negocioRepository, mock(ControlPlanService.class),
                        proyectoRepository, datosDeProyecto(), mock(ZonaRepository.class),
                        mock(AgregadoZonaRepository.class), avisoRepository, generador, Clock.systemUTC())
                .execute(new ExportarResumenRequest(USUARIO_ID, PROYECTO_ID, null, null));
    }

    private void cuandoSeObtieneElResumenDeInicio() {
        resumenInicio = config.obtenerResumenInicio(negocioRepository, proyectoRepository, datosDeProyecto(),
                Clock.systemUTC()).execute(new ObtenerResumenInicioRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeObtienenLasMetricasDelMes() {
        metricas = config.obtenerMetricas(negocioRepository, proyectoRepository, datosDeProyecto(),
                        mock(ZonaRepository.class), mock(AgregadoZonaRepository.class), avisoRepository, Clock.systemUTC())
                .execute(new ObtenerMetricasRequest(USUARIO_ID, PROYECTO_ID, PeriodoMetricas.MES));
    }

    private void cuandoSeObtieneElAvisoEconomico() {
        avisoEconomico = config.obtenerAvisoEconomico(negocioRepository, proyectoRepository, datosDeProyecto(),
                Clock.systemUTC()).execute(new ObtenerAvisoEconomicoRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void entoncesElReporteEstaVacioConMaximoDeFilas(int maximo) {
        assertThat(reporte.filas()).isEmpty();
        assertThat(reporte.maximoFilas()).isEqualTo(maximo);
    }

    private void entoncesElReporteExportadoSeLlamaYContiene(String prefijo, int... bytes) {
        assertThat(reporteExportado.contenido()).containsExactly(bytes);
        assertThat(reporteExportado.nombreArchivo()).startsWith(prefijo);
    }

    private void entoncesElResumenSeLlamaYContiene(String prefijo, int... bytes) {
        assertThat(resumenExportado.contenido()).containsExactly(bytes);
        assertThat(resumenExportado.nombreArchivo()).startsWith(prefijo);
    }

    private void entoncesElInicioEsDelSistemaSinVentas(String sistema) {
        assertThat(resumenInicio.sistema()).isEqualTo(sistema);
        assertThat(resumenInicio.hayVentas()).isFalse();
    }

    private void entoncesLasMetricasSonDelSistemaConDias(String sistema, int dias) {
        assertThat(metricas.sistema()).isEqualTo(sistema);
        assertThat(metricas.porDia()).hasSize(dias);
    }

    private void entoncesNoHayAvisoEconomico() {
        assertThat(avisoEconomico.aviso()).isNull();
    }

    private DatosDeProyectoService datosDeProyecto() {
        return config.datosDeProyectoService(mock(RegistroRepository.class), mock(VentaCajaRepository.class),
                mock(CobroCajaRepository.class));
    }

    private static GenerarReporteRequest unPedidoDeReporte() {
        return new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null);
    }
}