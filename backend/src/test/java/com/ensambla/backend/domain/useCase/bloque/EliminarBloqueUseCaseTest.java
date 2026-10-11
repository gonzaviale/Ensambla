package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.EliminarBloqueRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.service.bloque.BloqueService;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
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

class EliminarBloqueUseCaseTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final EliminarBloqueRequest REQUEST = new EliminarBloqueRequest(USUARIO_ID, BLOQUE_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final BloqueService bloqueService = mock(BloqueService.class);
    private final EliminarBloqueUseCase eliminarBloqueUseCase =
            new BloqueUseCaseConfig().eliminarBloqueUseCase(negocioService, bloqueService);

    @Test
    @DisplayName("Elimina el bloque obtenido dentro del negocio de la sesión")
    void executeShouldDeleteBloqueOfSessionNegocio() {
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenReturn(bloque);

        eliminarBloqueUseCase.execute(REQUEST);

        verify(bloqueService).eliminar(bloque);
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 y no borra nada")
    void executeShouldNotDeleteForeignBloque() {
        givenNegocio();
        when(bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID)).thenThrow(new ResourceNotFoundException("El bloque no existe"));

        assertThatThrownBy(() -> eliminarBloqueUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bloqueService, never()).eliminar(any());
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }
}
