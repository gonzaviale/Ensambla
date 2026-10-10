package com.ensambla.backend.domain.useCase.proyecto;

import com.ensambla.backend.domain.dto.proyecto.EliminarProyectoRequest;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.ProyectoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarProyectoUseCaseTest {

    @Test
    @DisplayName("Eliminar usa exclusivamente el negocio del usuario de la sesión")
    void executeShouldDeleteForSessionNegocio() {
        NegocioService negocioService = mock(NegocioService.class);
        ProyectoService proyectoService = mock(ProyectoService.class);
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
        var useCase = new ProyectoUseCaseConfig().eliminarProyectoUseCase(negocioService, proyectoService);

        useCase.execute(new EliminarProyectoRequest(USUARIO_ID, PROYECTO_ID));

        verify(proyectoService).eliminar(NEGOCIO_ID, PROYECTO_ID);
    }
}

