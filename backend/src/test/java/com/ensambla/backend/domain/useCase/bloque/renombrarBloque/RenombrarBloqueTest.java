package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarBloqueTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final RenombrarBloqueRequest REQUEST =
            new RenombrarBloqueRequest(USUARIO_ID, BLOQUE_ID, "Compradores");

    private final NegocioService negocioService = mock(NegocioService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final BloqueUseCaseConfig config = new BloqueUseCaseConfig();
    private final RenombrarBloque renombrarBloque = config.renombrarBloque(
            negocioService,
            bloqueService,
            proyectoService,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Renombra validando contra los bloques del proyecto dueño y con la hora del reloj")
    void executeShouldRenameAgainstOwnerProyecto() {
        Bloque bloque = givenBloque("Clientes");
        Proyecto proyecto = ProyectoTestData.proyecto();
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(bloqueService.renombrar(proyecto, bloque, "Compradores", AHORA)).thenReturn(givenBloque("Compradores"));

        BloqueItem result = renombrarBloque.execute(REQUEST);

        assertThat(result.id()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombre()).isEqualTo("Compradores");
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 sin consultar el proyecto ni renombrar")
    void executeShouldStopWhenBloqueIsForeign() {
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> renombrarBloque.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoService);
        verify(bloqueService, never()).renombrar(any(), any(), any(), any());
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Bloque givenBloque(String nombre) {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .negocioId(NEGOCIO_ID)
                .proyectoId(PROYECTO_ID)
                .nombre(nombre)
                .posicion((short) 0)
                .build();
    }
}
