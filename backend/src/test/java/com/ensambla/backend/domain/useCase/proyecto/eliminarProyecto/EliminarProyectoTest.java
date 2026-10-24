package com.ensambla.backend.domain.useCase.proyecto.eliminarProyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.plan.ControlPlanService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarProyectoTest {

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final EliminarProyecto eliminarProyecto = new ProyectoUseCaseConfig().eliminarProyecto(
            negocioService,
            new ProyectoService(proyectoRepository, mock(ControlPlanService.class), new TransaccionDirecta()),
            proyectoRepository,
            new TransaccionDirecta());

    @BeforeEach
    void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Eliminar valida pertenencia y borra filtrando por el negocio de la sesión")
    void executeShouldDeleteForSessionNegocio() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID))
                .thenReturn(Optional.of(ProyectoTestData.proyecto()));

        eliminarProyecto.execute(new EliminarProyectoRequest(USUARIO_ID, PROYECTO_ID));

        verify(proyectoRepository).deleteByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID);
    }

    @Test
    @DisplayName("Eliminar un proyecto ajeno no ejecuta el borrado")
    void executeShouldNotDeleteForeignProyecto() {
        when(proyectoRepository.findByIdAndNegocioId(PROYECTO_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eliminarProyecto.execute(new EliminarProyectoRequest(USUARIO_ID, PROYECTO_ID)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(proyectoRepository, never()).deleteByIdAndNegocioId(any(), any());
    }
}
