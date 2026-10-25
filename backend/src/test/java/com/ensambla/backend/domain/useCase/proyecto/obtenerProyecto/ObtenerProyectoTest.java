package com.ensambla.backend.domain.useCase.proyecto.obtenerProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ObtenerProyectoTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final PlantillaRepository plantillaRepository = mock(PlantillaRepository.class);
    private final ProyectoUseCaseConfig config = new ProyectoUseCaseConfig();
    private final ObtenerProyecto obtenerProyecto = config.obtenerProyecto(
            negocioRepository,
            proyectoRepository);

    @Test
    @DisplayName("Resuelve el negocio de sesión, delega y mapea la respuesta")
    void executeShouldResolveNegocioAndMapResponse() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(ProyectoTestData.proyecto());

        var result = obtenerProyecto.execute(new ObtenerProyectoRequest(USUARIO_ID, PROYECTO_ID));

        assertThat(result.id()).isEqualTo(PROYECTO_ID);
        verify(proyectoRepository).obtener(NEGOCIO_ID, PROYECTO_ID);
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> obtenerProyecto.execute(new ObtenerProyectoRequest(USUARIO_ID, PROYECTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoRepository, plantillaRepository);
    }
}

