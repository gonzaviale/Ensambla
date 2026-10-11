package com.ensambla.backend.domain.useCase.campo;

import com.ensambla.backend.domain.dto.campo.EliminarCampoRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.service.campo.CampoService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.CampoUseCaseConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EliminarCampoUseCaseTest {

    private static final UUID CAMPO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final EliminarCampoRequest REQUEST = new EliminarCampoRequest(USUARIO_ID, CAMPO_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final CampoService campoService = mock(CampoService.class);
    private final EliminarCampoUseCase eliminarCampoUseCase =
            new CampoUseCaseConfig().eliminarCampoUseCase(negocioService, campoService);

    @Test
    @DisplayName("Elimina el campo del negocio de la sesión")
    void executeShouldDeleteCampoOfSessionNegocio() {
        Campo campo = Campo.builder().id(CAMPO_ID).negocioId(NEGOCIO_ID).build();
        givenNegocio();
        when(campoService.obtener(CAMPO_ID, NEGOCIO_ID)).thenReturn(campo);

        eliminarCampoUseCase.execute(REQUEST);

        verify(campoService).eliminar(campo);
    }

    @Test
    @DisplayName("Un campo ajeno propaga el 404 y no borra nada")
    void executeShouldNotDeleteWhenCampoIsForeign() {
        givenNegocio();
        when(campoService.obtener(CAMPO_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El campo no existe"));

        assertThatThrownBy(() -> eliminarCampoUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(campoService, never()).eliminar(any());
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }
}
