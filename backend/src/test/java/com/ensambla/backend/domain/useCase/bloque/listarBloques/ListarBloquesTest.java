package com.ensambla.backend.domain.useCase.bloque.listarBloques;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.proyecto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ListarBloquesTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final ListarBloques listarBloques = new BloqueUseCaseConfig().listarBloques(
            negocioRepository,
            proyectoRepository);

    private List<BloqueItem> bloques;
    private Throwable rechazo;

    @BeforeEach
    void dadoUnNegocioDelUsuario() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los bloques del proyecto del negocio de la sesión en su orden")
    void listaLosBloquesDelProyectoDelNegocioDeLaSesionEnSuOrden() {
        dadoUnProyectoConProductosYUnBloqueVacio();

        cuandoSeListanLosBloques();

        entoncesSeListanProductosYVacioDelProyecto();
    }

    @Test
    @DisplayName("Un proyecto ajeno propaga el 404 sin convertirlo")
    void unProyectoAjenoPropagaElNoEncontradoSinConvertirlo() {
        dadoUnProyectoAjeno();

        cuandoSeIntentaListarLosBloques();

        entoncesSeRechazaPorqueElProyectoNoExiste();
    }

    // --- dado ---
    private void dadoUnProyectoConProductosYUnBloqueVacio() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto());
    }

    private void dadoUnProyectoAjeno() {
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));
    }

    // --- cuando ---
    private void cuandoSeListanLosBloques() {
        bloques = listarBloques.execute(new ListarBloquesRequest(USUARIO_ID, PROYECTO_ID));
    }

    private void cuandoSeIntentaListarLosBloques() {
        rechazo = catchThrowable(this::cuandoSeListanLosBloques);
    }

    // --- entonces ---
    private void entoncesSeListanProductosYVacioDelProyecto() {
        assertThat(bloques).extracting(BloqueItem::nombre).containsExactly("Productos", "Vacío");
        assertThat(bloques).extracting(BloqueItem::proyectoId).containsOnly(PROYECTO_ID);
    }

    private void entoncesSeRechazaPorqueElProyectoNoExiste() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
