package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.CrearProyectoDesdePlantillaRequest;
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
import static com.ensambla.backend.support.ProyectoTestData.PLANTILLA_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearProyectoDesdePlantillaUseCaseTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final PlantillaService plantillaService = mock(PlantillaService.class);
    private final ProyectoUseCaseConfig config = new ProyectoUseCaseConfig();
    private final CrearProyectoDesdePlantillaUseCase crearProyectoDesdePlantillaUseCase = config.crearProyectoDesdePlantillaUseCase(
            negocioService,
            proyectoService,
            plantillaService,
            config.crearProyectoDesdePlantillaMapper(config.proyectoItemMapper()),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Resuelve el negocio de sesión, delega y mapea la respuesta")
    void executeShouldResolveNegocioAndMapResponse() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(plantillaService.obtenerParaProyecto(PLANTILLA_ID)).thenReturn(ProyectoTestData.plantilla());
        when(proyectoService.crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA)).thenReturn(ProyectoTestData.proyecto());

        var result = crearProyectoDesdePlantillaUseCase.execute(new CrearProyectoDesdePlantillaRequest(USUARIO_ID, PLANTILLA_ID));

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        verify(proyectoService).crearDesdePlantilla(NEGOCIO_ID, ProyectoTestData.plantilla(), AHORA);
        assertThat(config.clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> crearProyectoDesdePlantillaUseCase.execute(new CrearProyectoDesdePlantillaRequest(USUARIO_ID, PLANTILLA_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoService, plantillaService);
    }
}

