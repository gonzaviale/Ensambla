package com.ensambla.backend.infrastructure.config.useCase;

import com.ensambla.backend.domain.dto.receta.GuardarRecetaRequest;
import com.ensambla.backend.domain.dto.receta.ObtenerRecetaRequest;
import com.ensambla.backend.domain.dto.receta.RecetaItem;
import com.ensambla.backend.domain.mapper.receta.RecetaItemMapper;
import com.ensambla.backend.domain.model.receta.Receta;
import com.ensambla.backend.domain.model.receta.RecetaCosteada;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.receta.RecetaService;
import com.ensambla.backend.domain.service.registro.RegistroDatosService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecetaUseCaseConfigTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final UUID PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    private final RecetaUseCaseConfig config = new RecetaUseCaseConfig();
    private final RecetaItemMapper recetaItemMapper = config.recetaItemMapper();

    private NegocioService negocioService;
    private RegistroDatosService registroDatosService;
    private RecetaService recetaService;

    @BeforeEach
    void setUp() {
        negocioService = mock(NegocioService.class);
        registroDatosService = mock(RegistroDatosService.class);
        recetaService = mock(RecetaService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(registroDatosService.obtener(PRODUCTO_ID, NEGOCIO_ID))
                .thenReturn(Registro.builder().id(PRODUCTO_ID).build());
        when(recetaService.costear(any(), any())).thenReturn(RecetaCosteada.vacia());
    }

    @Test
    @DisplayName("El wiring de obtener conecta los servicios y el mapper")
    void obtenerRecetaUseCaseShouldWireCollaborators() {
        when(recetaService.obtener(PRODUCTO_ID, NEGOCIO_ID)).thenReturn(Receta.builder().lineas(List.of()).build());

        RecetaItem response = config.obtenerRecetaUseCase(
                        negocioService, registroDatosService, recetaService,
                        config.obtenerRecetaMapper(recetaItemMapper))
                .execute(new ObtenerRecetaRequest(USUARIO_ID, PRODUCTO_ID));

        assertThat(response.lineas()).isEmpty();
        assertThat(response.costo()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El wiring de guardar conecta los servicios, el mapper y el reloj")
    void guardarRecetaUseCaseShouldWireCollaborators() {
        when(recetaService.guardar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RecetaItem response = config.guardarRecetaUseCase(
                        negocioService, registroDatosService, recetaService,
                        config.guardarRecetaMapper(recetaItemMapper), Clock.systemUTC())
                .execute(new GuardarRecetaRequest(USUARIO_ID, PRODUCTO_ID, null, List.of()));

        assertThat(response.lineas()).isEmpty();
    }
}
