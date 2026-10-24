package com.ensambla.backend.domain.useCase.proyecto.renombrarProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plantilla.PlantillaService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarProyectoTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final PlantillaService plantillaService = mock(PlantillaService.class);
    private final ProyectoUseCaseConfig config = new ProyectoUseCaseConfig();
    private final RenombrarProyecto renombrarProyecto = config.renombrarProyecto(
            negocioService,
            proyectoService,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Resuelve el negocio de sesión, delega y mapea la respuesta")
    void executeShouldResolveNegocioAndMapResponse() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoService.renombrar(NEGOCIO_ID, PROYECTO_ID, "Nuevo", AHORA)).thenReturn(ProyectoTestData.proyecto());

        var result = renombrarProyecto.execute(new RenombrarProyectoRequest(USUARIO_ID, PROYECTO_ID, "Nuevo"));

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        verify(proyectoService).renombrar(NEGOCIO_ID, PROYECTO_ID, "Nuevo", AHORA);
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> renombrarProyecto.execute(
                new RenombrarProyectoRequest(USUARIO_ID, PROYECTO_ID, "Nuevo")))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoService, plantillaService);
    }
}

