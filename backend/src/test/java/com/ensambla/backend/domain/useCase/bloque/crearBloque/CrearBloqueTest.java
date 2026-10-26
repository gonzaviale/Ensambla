package com.ensambla.backend.domain.useCase.bloque.crearBloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import com.ensambla.backend.domain.repository.proyecto.ProyectoRepository;
import com.ensambla.backend.domain.useCase.bloque.BloqueItem;
import com.ensambla.backend.infrastructure.config.useCase.BloqueUseCaseConfig;
import com.ensambla.backend.support.ProyectoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.ProyectoTestData.AHORA;
import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.PROYECTO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CrearBloqueTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final CrearBloqueRequest REQUEST = new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Clientes");

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final ProyectoRepository proyectoRepository = mock(ProyectoRepository.class);
    private final BloqueRepository bloqueRepository = mock(BloqueRepository.class);
    private final BloqueUseCaseConfig config = new BloqueUseCaseConfig();
    private final CrearBloque crearBloque = config.crearBloque(
            negocioRepository,
            proyectoRepository,
            bloqueRepository,
            Clock.fixed(AHORA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Crea el bloque en el proyecto del negocio de la sesión con la hora del reloj")
    void executeShouldCreateBloqueInSessionProyecto() {
        Proyecto proyecto = ProyectoTestData.proyecto();
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(proyecto);
        when(bloqueRepository.save(any(Bloque.class))).thenReturn(givenBloqueGuardado());

        BloqueItem result = crearBloque.execute(REQUEST);

        assertThat(result.id()).isEqualTo(BLOQUE_ID);
        assertThat(result.nombre()).isEqualTo("Clientes");
    }

    @Test
    @DisplayName("Guarda el bloque que arma el proyecto al final de su lista")
    void executeShouldSaveBloqueBuiltByProyecto() {
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(ProyectoTestData.proyecto());
        when(bloqueRepository.save(any(Bloque.class))).thenAnswer(invocation -> invocation.getArgument(0));

        crearBloque.execute(REQUEST);

        ArgumentCaptor<Bloque> guardado = ArgumentCaptor.forClass(Bloque.class);
        verify(bloqueRepository).save(guardado.capture());
        assertThat(guardado.getValue().getNombre()).isEqualTo("Clientes");
        assertThat(guardado.getValue().getProyectoId()).isEqualTo(ProyectoTestData.PROYECTO_ID);
        assertThat(guardado.getValue().getPosicion()).isEqualTo((short) 2);
        assertThat(guardado.getValue().getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Con un nombre repetido en el proyecto no guarda nada")
    void executeShouldNotSaveWhenNameIsTaken() {
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID)).thenReturn(ProyectoTestData.proyecto());

        assertThatThrownBy(() -> crearBloque.execute(new CrearBloqueRequest(USUARIO_ID, PROYECTO_ID, "Productos")))
                .isInstanceOf(DuplicateResourceException.class);
        verifyNoInteractions(bloqueRepository);
    }

    @Test
    @DisplayName("Si el proyecto es ajeno no crea ningún bloque")
    void executeShouldNotCreateWhenProyectoIsForeign() {
        givenNegocio();
        when(proyectoRepository.obtener(NEGOCIO_ID, PROYECTO_ID))
                .thenThrow(new ResourceNotFoundException("Proyecto no encontrado"));

        assertThatThrownBy(() -> crearBloque.execute(REQUEST))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(bloqueRepository);
    }

    // --- arrange ---
    private void givenNegocio() {
        when(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).thenReturn(NEGOCIO_ID);
    }

    private Bloque givenBloqueGuardado() {
        return Bloque.builder()
                .id(BLOQUE_ID)
                .proyectoId(PROYECTO_ID)
                .nombre("Clientes")
                .posicion((short) 2)
                .build();
    }
}
