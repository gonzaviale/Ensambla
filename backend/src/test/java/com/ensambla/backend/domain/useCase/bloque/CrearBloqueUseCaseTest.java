package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.CrearBloqueRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearBloqueUseCaseTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final CrearBloqueRequest REQUEST = new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Clientes");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final BloqueUseCaseConfig config = new BloqueUseCaseConfig();
    private final CrearBloqueUseCase crearBloqueUseCase = config.crearBloqueUseCase(
            negocioService,
            proyectoService,
            bloqueService,
            config.crearBloqueMapper(config.bloqueItemMapper()),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Crea el bloque en el proyecto del negocio de la sesión con la hora del reloj")
    void executeShouldCreateBloqueInSessionProyecto() {
        Proyecto proyecto = ProyectoTestData.proyecto();
        givenNegocio();
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(bloqueService.crear(proyecto, "Clientes", AHORA)).thenReturn(givenBloqueGuardado());

        BloqueItem result = crearBloqueUseCase.execute(REQUEST);

        assertThat(result.id()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombre()).isEqualTo("Clientes");
    }

    @Test
    @DisplayName("Si el proyecto es ajeno no crea ningún bloque")
    void executeShouldNotCreateWhenProyectoIsForeign() {
        givenNegocio();
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> crearBloqueUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(bloqueService);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Bloque givenBloqueGuardado() {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .proyectoId(PROYECTO_ID)
                .nombre("Clientes")
                .posicion((short) 2)
                .build();
    }
}
