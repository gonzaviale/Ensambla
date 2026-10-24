package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import com.ensambla.backend.domain.repository.campo.CampoRepository;
import com.ensambla.backend.domain.repository.registro.RegistroRepository;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.reporte.ReporteService;
import com.ensambla.backend.domain.useCase.reporte.GenerarReporteRequest;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteUseCaseConfigTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");

    private final ReporteUseCaseConfig config = new ReporteUseCaseConfig();

    private NegocioService negocioService;
    private BloqueService bloqueService;
    private CampoService campoService;
    private ReporteService reporteService;

    @BeforeEach
    void setUp() {
        negocioService = mock(NegocioService.class);
        bloqueService = mock(BloqueService.class);
        campoService = mock(CampoService.class);
        reporteService = mock(ReporteService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .thenReturn(Bloque.builder().id(BLOQUE_ID).nombre("Pedidos").build());
        when(campoService.listarDeBloque(BLOQUE_ID, NEGOCIO_ID)).thenReturn(List.of());
        when(reporteService.generar(any(), any(), anyInt()))
                .thenReturn(new ReporteRegistros(List.of(), new TotalesRegistros(0, Map.of()), false));
    }

    @Test
    @DisplayName("El wiring de generar conecta los servicios y el mapper")
    void generarReporteUseCaseShouldWireCollaborators() {
        GenerarReporteResponse response = config.generarReporte(
                        negocioService, mock(ControlPlanService.class), Clock.systemUTC(),
                        bloqueService, campoService, reporteService)
                .execute(new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));

        assertThat(response.filas()).isEmpty();
        assertThat(response.maximoFilas()).isEqualTo(50);
    }

    @Test
    @DisplayName("El wiring de exportar conecta los servicios, el puerto del archivo, el mapper y el reloj")
    void exportarReporteUseCaseShouldWireCollaborators() {
        ExportadorReportePort exportador = tabla -> new byte[] {1, 2};

        ExportarReporteResponse response = config.exportarReporte(
                        negocioService, mock(ControlPlanService.class), bloqueService, campoService, reporteService,
                        mock(RegistroRepository.class), mock(CampoRepository.class), exportador, Clock.systemUTC())
                .execute(new GenerarReporteRequest(USUARIO_ID, BLOQUE_ID, null, null, null, null, null));

        assertThat(response.contenido()).containsExactly(1, 2);
        assertThat(response.nombreArchivo()).startsWith("reporte-pedidos-");
    }
}
