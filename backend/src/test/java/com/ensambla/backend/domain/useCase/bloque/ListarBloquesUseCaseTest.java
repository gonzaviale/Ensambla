package com.ensambla.backend.domain.useCase.bloque;

import com.ensambla.backend.domain.dto.bloque.BloqueItem;
import com.ensambla.backend.domain.dto.bloque.ListarBloquesRequest;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.service.negocio.NegocioService;
import com.ensambla.backend.domain.service.proyecto.ProyectoService;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
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
import static org.mockito.Mockito.when;

class ListarBloquesUseCaseTest {

    private static final ListarBloquesRequest REQUEST = new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID);

    private final NegocioService negocioService = mock(NegocioService.class);
    private final ProyectoService proyectoService = mock(ProyectoService.class);
    private final BloqueUseCaseConfig config = new BloqueUseCaseConfig();
    private final ListarBloquesUseCase listarBloquesUseCase = config.listarBloquesUseCase(
            negocioService,
            proyectoService,
            config.listarBloquesMapper(config.bloqueItemMapper()));

    @Test
    @DisplayName("Lista los bloques del proyecto del negocio de la sesión en su orden")
    void executeShouldListBloquesOfSessionProyecto() {
        givenNegocio();
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(ProyectoTestData.proyecto());

        List<BloqueItem> result = listarBloquesUseCase.execute(REQUEST);

        assertThat(result).extracting(BloqueItem::nombre).containsExactly("Productos", "Vacío");
        assertThat(result).extracting(BloqueItem::proyectoId).containsOnly(PROYECTO_ID);
    }

    @Test
    @DisplayName("Un proyecto ajeno propaga el 404 sin convertirlo")
    void executeShouldPropagateNotFoundWhenProyectoIsForeign() {
        givenNegocio();
        when(proyectoService.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> listarBloquesUseCase.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioService.obtenerDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }
}
