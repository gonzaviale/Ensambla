package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.reporte.DatosDeProyectoService;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ComoVieneElNegocioUseCaseConfigTest {

    private static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");

    private final ComoVieneElNegocioUseCaseConfig config = new ComoVieneElNegocioUseCaseConfig();

    @Test
    @DisplayName("El wiring conecta el negocio, sus sistemas, el servicio de datos y el reloj")
    void obtenerComoVieneElNegocioShouldWireCollaborators() {
        NegocioRepository negocioRepository = mock(NegocioRepository.class);
        ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
        when(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .thenReturn(Negocio.builder().id(NEGOCIO_ID).nombre("Almacén de Ana").build());
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of());

        ObtenerComoVieneElNegocioResponse response = config.obtenerComoVieneElNegocio(
                        negocioRepository, proyectoRepository, mock(DatosDeProyectoService.class), Clock.systemUTC())
                .execute(new ObtenerComoVieneElNegocioRequest(USUARIO_ID));

        assertThat(response.hayVentas()).isFalse();
    }
}
