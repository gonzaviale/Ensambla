package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
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

class ListarBloquesTest {

    private static final ListarBloquesRequest REQUEST = new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID);

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final BloqueUseCaseConfig config = new BloqueUseCaseConfig();
    private final ListarBloques listarBloques = config.listarBloques(
            negocioRepository,
            proyectoRepository);

    @Test
    @DisplayName("Lista los bloques del proyecto del negocio de la sesión en su orden")
    void executeShouldListBloquesOfSessionProyecto() {
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(ProyectoTestData.proyecto());

        List<BloqueItem> result = listarBloques.execute(REQUEST);

        assertThat(result).extracting(BloqueItem::nombre).containsExactly("Productos", "Vacío");
        assertThat(result).extracting(BloqueItem::proyectoId).containsOnly(PROYECTO_ID);
    }

    @Test
    @DisplayName("Un proyecto ajeno propaga el 404 sin convertirlo")
    void executeShouldPropagateNotFoundWhenProyectoIsForeign() {
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> listarBloques.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }
}
