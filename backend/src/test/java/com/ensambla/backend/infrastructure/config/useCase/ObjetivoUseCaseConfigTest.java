package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.aviso.MetricasVentas;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.sistema.SistemaRepository;
import com.ensambla.backend.domain.repository.zona.AgregadoZonaRepository;
import com.ensambla.backend.domain.repository.zona.ZonaRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasosResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObjetivoUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final ObjetivoUseCaseConfig config = new ObjetivoUseCaseConfig();

    @Test
    @DisplayName("El wiring de pasos conecta las fuentes reales del negocio")
    void obtenerPasosShouldWireBusinessSources() {
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        SistemaRepository sistemaRepository = mock(SistemaRepository.class);
        DatosDeProyectoService datosDeProyectoService = mock(DatosDeProyectoService.class);
        ZonaRepository zonaRepository = mock(ZonaRepository.class);
        AgregadoZonaRepository agregadoZonaRepository = mock(AgregadoZonaRepository.class);
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(sistemaRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());
        when(negocioRepository.findZonaIdById(NEGOCIO_ID)).thenReturn(Optional.empty());

        ObtenerPasosResponse response = config.obtenerPasos(
                        negocioRepository,
                        sistemaRepository,
                        datosDeProyectoService,
                        zonaRepository,
                        agregadoZonaRepository,
                        new MetricasVentas(30, new BigDecimal("0.5")),
                        Clock.systemUTC())
                .execute(new ObtenerPasosRequest(USUARIO_ID));

        assertThat(response.content()).isEmpty();
    }
}
