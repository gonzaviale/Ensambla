package com.ensambla.backend.domain.useCase.bloque.renombrarBloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RenombrarBloqueTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class, CALLS_REAL_METHODS);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final RenombrarBloque renombrarBloque = new BloqueUseCaseConfig().renombrarBloque(
            negocioRepository,
            proyectoRepository,
            bloqueRepository,
            new TransaccionDirecta(),
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    private final Proyecto proyecto = ProyectoTestData.proyecto();

    @BeforeEach
    void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Renombra validando contra los bloques del proyecto dueño y guarda con la hora del reloj")
    void executeShouldRenameAgainstOwnerProyecto() {
        Bloque bloque = givenBloqueDelProyecto(0);
        when(bloqueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BloqueItem result = renombrarBloque.execute(new RenombrarBloqueRequest(USUARIO_ID, bloque.getId(), " Mercadería "));

        assertThat(result.id()).isEqualTo(bloque.getId());
        assertThat(result.nombre()).isEqualTo("Mercadería");
        assertThat(result.actualizadoEn()).isEqualTo(AHORA);
        verify(bloqueRepository).save(bloque);
    }

    @Test
    @DisplayName("Renombrar con el nombre de otro bloque no modifica ni guarda el bloque")
    void executeShouldNotSaveWhenNameIsTaken() {
        Bloque bloque = givenBloqueDelProyecto(1);

        assertThatThrownBy(() -> renombrarBloque.execute(
                new RenombrarBloqueRequest(USUARIO_ID, bloque.getId(), "Productos")))
                .isInstanceOf(DuplicateResourceException.class);
        assertThat(bloque.getNombre()).isEqualTo("Vacío");
        verify(bloqueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Un bloque ajeno propaga el 404 sin consultar el proyecto ni renombrar")
    void executeShouldStopWhenBloqueIsForeign() {
        Bloque bloque = proyecto.getBloques().getFirst();
        when(bloqueRepository.findByIdAndNegocioId(bloque.getId(), NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> renombrarBloque.execute(
                new RenombrarBloqueRequest(USUARIO_ID, bloque.getId(), "Compradores")))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(proyectoRepository);
        verify(bloqueRepository, never()).save(any());
    }

    // --- arrange ---
    private Bloque givenBloqueDelProyecto(int indice) {
        Bloque bloque = proyecto.getBloques().get(indice);
        when(bloqueRepository.findByIdAndNegocioId(bloque.getId(), NEGOCIO_ID)).thenReturn(Optional.of(bloque));
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        return bloque;
    }
}
