package com.ensambla.backend.domain.useCase.proyecto.listarProyectos;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ListarProyectosTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ProyectoUseCaseConfig config = new ProyectoUseCaseConfig();
    private final ListarProyectos listarProyectos = config.listarProyectos(
            negocioService,
            proyectoRepository);

    @Test
    @DisplayName("Resuelve el negocio de sesión y mapea sus proyectos")
    void executeShouldResolveNegocioAndMapResponse() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        when(proyectoRepository.findAllByNegocioId(NEGOCIO_ID)).thenReturn(List.of(ProyectoTestData.proyecto()));

        var result = listarProyectos.execute(new ListarProyectosRequest(USUARIO_ID));

        assertThat(result.getFirst().id()).isEqualTo(PROYECTO_ID);
        verify(proyectoRepository).findAllByNegocioId(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Si no existe el negocio no consulta ni escribe proyectos")
    void executeShouldStopWhenNegocioMissing() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenThrow(new ResourceNotFoundException("Sin negocio"));

        assertThatThrownBy(() -> listarProyectos.execute(new ListarProyectosRequest(USUARIO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoRepository);
    }
}

