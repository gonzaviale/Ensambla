package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.plan.FuncionPlan;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.reporte.DatosDeProyecto;
import com.ensambla.backend.domain.model.reporte.ResumenNegocio;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.zona.AgregadoZonaProducto;
import com.ensambla.backend.domain.model.zona.AnalisisZona;
import com.ensambla.backend.domain.model.zona.Zona;
import com.ensambla.backend.domain.model.zona.ZonaId;
import com.ensambla.backend.domain.port.GeneradorResumenPdfPort;
import com.ensambla.backend.domain.repository.aviso.AvisoRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ContextoDeResumenService;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenNegocio;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarResumen.ExportarResumenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportarResumenNegocioTest {

    private ExportarResumenResponse response;
    private Zona zona;

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final ProyectoId PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000001");
    private static final ProyectoId OTRO_PROYECTO_ID = ProyectoId.de("c0000000-0000-4000-8000-000000000002");
    private static final ZonaId ZONA_ID = ZonaId.de("e0000000-0000-4000-8000-000000000001");
    /** En UTC ya es el 20, pero en Argentina todavía es el 19. */
    private static final Instant INSTANTE = Instant.parse("2026-10-20T01:00:00Z");
    private static final byte[] PDF = {'%', 'P', 'D', 'F'};

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ControlPlanService controlPlanService = mock(ControlPlanService.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
    private final ZonaRepository zonaRepository = mock(ZonaRepository.class);
    private final AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
    private final AvisoRepository avisoRepository = mock(AvisoRepository.class);
    private final GeneradorResumenPdfPort generador = mock(GeneradorResumenPdfPort.class);

    private final ExportarResumenNegocio exportarResumenNegocio = new ExportarResumenNegocio(
            negocioRepository, controlPlanService, proyectoRepository, datosDeProyectoService,
            new ContextoDeResumenService(negocioRepository, zonaRepository, agregadoZonaRepository, avisoRepository),
            generador, Clock.fixed(INSTANTE, ZoneOffset.UTC));

    private Throwable excepcion;

    @BeforeEach
    void setUp() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build();
        Proyecto proyecto = Proyecto.builder().id(PROYECTO_ID).negocioId(NEGOCIO_ID).nombre("Almacén de prueba").build();
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).thenReturn(negocio);
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(datosDeProyectoService.cargar(proyecto, NEGOCIO_ID)).thenReturn(
                new DatosDeProyecto("Almacén de prueba", List.of(), List.of(), List.of()));
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(generador.generar(any(ResumenNegocio.class))).thenReturn(PDF);
    }

    @Test
    @DisplayName("Devuelve el PDF con el nombre del sistema y la fecha argentina, para los últimos 30 días")
    void devuelveElPDFConElNombreDelSistemaYLaFechaArgentinaParaLosUltimos30Dias() {
        cuandoSeEjecutaDevuelveElPDFConElNombreDelSistemaYLaFechaArgentinaParaLosUltimos30();

        entoncesSeVerificaDevuelveElPDFConElNombreDelSistemaYLaFechaArgentinaParaLosUltimos30();
    }

    @Test
    @DisplayName("Usa el período que pidió la persona")
    void usaElPeriodoQuePidioLaPersona() {
        cuandoSeEjecutaUsaElPeriodoQuePidioLaPersona();

        entoncesSeVerificaUsaElPeriodoQuePidioLaPersona();
    }

    @Test
    @DisplayName("Exige la función de reportes del plan antes de armar nada")
    void exigeLaFuncionDeReportesDelPlanAntesDeArmarNada() {
        dadoElEscenarioDeExigeLaFuncionDeReportesDelPlanAntesDeArmarNada();

        cuandoSeEjecutaExigeLaFuncionDeReportesDelPlanAntesDeArmarNada();

        entoncesSeVerificaExigeLaFuncionDeReportesDelPlanAntesDeArmarNada();
    }

    @Test
    @DisplayName("Un sistema de otro negocio responde como inexistente")
    void unSistemaDeOtroNegocioRespondeComoInexistente() {
        dadoUnSistemaDeOtroNegocio();

        cuandoSeIntentaExportarElResumenDelSistemaAjeno();

        entoncesSeRechazaPorqueElSistemaNoExiste();
    }

    @Test
    @DisplayName("Rechaza un período al revés o de más de un año sin generar el PDF")
    void rechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF() {
        dadoElEscenarioDeRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF();

        cuandoSeEjecutaRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF();

        entoncesSeVerificaRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF();
    }

    @Test
    @DisplayName("Le pasa al resumen los agregados de la zona del negocio")
    void lePasaAlResumenLosAgregadosDeLaZonaDelNegocio() {
        dadoElEscenarioDeLePasaAlResumenLosAgregadosDeLaZonaDelNegocio();

        cuandoSeEjecutaLePasaAlResumenLosAgregadosDeLaZonaDelNegocio();

        entoncesSeVerificaLePasaAlResumenLosAgregadosDeLaZonaDelNegocio();
    }

    @Test
    @DisplayName("Sólo cuenta los avisos del sistema pedido")
    void soloCuentaLosAvisosDelSistemaPedido() {
        dadoElEscenarioDeSoloCuentaLosAvisosDelSistemaPedido();

        cuandoSeEjecutaSoloCuentaLosAvisosDelSistemaPedido();

        entoncesSeVerificaSoloCuentaLosAvisosDelSistemaPedido();
    }

    // --- cuando ---
    private ExportarResumenResponse cuandoExportar(LocalDate desde, LocalDate hasta) {
        return exportarResumenNegocio.execute(new ExportarResumenRequest(USUARIO_ID, PROYECTO_ID, desde, hasta));
    }

    private ResumenNegocio resumenGenerado() {
        ArgumentCaptor<ResumenNegocio> captor = ArgumentCaptor.forClass(ResumenNegocio.class);
        verify(generador).generar(captor.capture());
        return captor.getValue();
    }

    private static Aviso aviso(ProyectoId proyectoId) {
        return Aviso.builder().proyectoId(proyectoId).tipo(TipoAviso.STOCK).estado(EstadoAviso.PENDIENTE).build();
    }

    // --- dado ---

    private void dadoElEscenarioDeExigeLaFuncionDeReportesDelPlanAntesDeArmarNada() {
        doThrow(new BusinessRuleException("Es una función Premium")).when(controlPlanService)
                        .validarFuncion(any(), any(), any());
    }

    private void dadoElEscenarioDeRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF() {
        assertThatThrownBy(() -> cuandoExportar(LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 7)))
                        .isInstanceOf(BusinessRuleException.class);
    }

    private void dadoElEscenarioDeLePasaAlResumenLosAgregadosDeLaZonaDelNegocio() {
        zona = Zona.builder().id(ZONA_ID).nombre("San Justo").build();
        AnalisisZona analisis = new AnalisisZona("San Justo", 8, List.of(new AgregadoZonaProducto("Yerba", 8, 8,
                        BigDecimal.ONE, BigDecimal.TWO, BigDecimal.TEN, BigDecimal.TEN)));
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.of(ZONA_ID));
        when(zonaRepository.findById(ZONA_ID)).thenReturn(Optional.of(zona));
        when(agregadoZonaRepository.analizar(zona)).thenReturn(analisis);
    }

    private void dadoElEscenarioDeSoloCuentaLosAvisosDelSistemaPedido() {
        when(avisoRepository.findByNegocioId(NEGOCIO_ID)).thenReturn(List.of(
                        aviso(PROYECTO_ID), aviso(OTRO_PROYECTO_ID), aviso(null)));
    }
    private void dadoUnSistemaDeOtroNegocio() {
        when(proyectoRepository.obtener(NEGOCIO_ID, OTRO_PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    // --- cuando ---

    private void cuandoSeEjecutaDevuelveElPDFConElNombreDelSistemaYLaFechaArgentinaParaLosUltimos30() {
        response = cuandoExportar(null, null);
    }

    private void cuandoSeEjecutaUsaElPeriodoQuePidioLaPersona() {
        cuandoExportar(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7));
    }

    private void cuandoSeEjecutaExigeLaFuncionDeReportesDelPlanAntesDeArmarNada() {
        assertThatThrownBy(() -> cuandoExportar(null, null)).isInstanceOf(BusinessRuleException.class);
    }

    private void cuandoSeEjecutaRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF() {
        assertThatThrownBy(() -> cuandoExportar(LocalDate.of(2025, 1, 1), LocalDate.of(2026, 10, 7)))
                        .isInstanceOf(BusinessRuleException.class);
    }

    private void cuandoSeEjecutaLePasaAlResumenLosAgregadosDeLaZonaDelNegocio() {
        cuandoExportar(null, null);
    }

    private void cuandoSeEjecutaSoloCuentaLosAvisosDelSistemaPedido() {
        cuandoExportar(null, null);
    }
    private void cuandoSeIntentaExportarElResumenDelSistemaAjeno() {
        excepcion = catchThrowable(() -> exportarResumenNegocio.execute(
                new ExportarResumenRequest(USUARIO_ID, OTRO_PROYECTO_ID, null, null)));
    }

    // --- entonces ---

    private void entoncesSeVerificaDevuelveElPDFConElNombreDelSistemaYLaFechaArgentinaParaLosUltimos30() {
        assertThat(response.contenido()).isEqualTo(PDF);
        assertThat(response.nombreArchivo()).isEqualTo("resumen-almacen-de-prueba-20261019.pdf");
        ResumenNegocio resumen = resumenGenerado();
        assertThat(resumen.negocio()).isEqualTo("Almacén de Ana");
        assertThat(resumen.sistema()).isEqualTo("Almacén de prueba");
        assertThat(resumen.emitidoEl()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(resumen.periodo().desde()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(resumen.periodo().hasta()).isEqualTo(LocalDate.of(2026, 10, 19));
    }

    private void entoncesSeVerificaUsaElPeriodoQuePidioLaPersona() {
        assertThat(resumenGenerado().periodo().dias()).isEqualTo(7);
    }

    private void entoncesSeVerificaExigeLaFuncionDeReportesDelPlanAntesDeArmarNada() {
        verify(controlPlanService).validarFuncion(NEGOCIO_ID, FuncionPlan.REPORTES_FILTRADOS,
                        OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC));
        verify(generador, never()).generar(any());
    }

    private void entoncesSeVerificaRechazaUnPeriodoAlRevesODeMasDeUnAnoSinGenerarElPDF() {
        verify(generador, never()).generar(any());
    }

    private void entoncesSeVerificaLePasaAlResumenLosAgregadosDeLaZonaDelNegocio() {
        verify(agregadoZonaRepository).analizar(zona);
    }

    private void entoncesSeVerificaSoloCuentaLosAvisosDelSistemaPedido() {
        assertThat(resumenGenerado().avisos().totalPendientes()).isEqualTo(1);
    }

    private void entoncesSeRechazaPorqueElSistemaNoExiste() {
        assertThat(excepcion).isInstanceOf(ResourceNotFoundException.class);
        verify(generador, never()).generar(any());
    }
}
