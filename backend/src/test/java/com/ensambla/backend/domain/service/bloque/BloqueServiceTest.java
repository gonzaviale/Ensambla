package com.ensambla.backend.domain.service.bloque;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.port.TransaccionPort;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import com.ensambla.backend.support.ProyectoTestData;
import com.ensambla.backend.support.TransaccionDirecta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueServiceTest {

    private static final BloqueId BLOQUE_ID = BloqueId.de("b0000000-0000-4000-8000-000000000001");
    private static final NegocioId NEGOCIO_ID = NegocioId.de("a0000000-0000-4000-8000-000000000001");
    private static final OffsetDateTime AHORA = OffsetDateTime.parse("2026-10-10T15:00:00Z");

    @Mock
    private BloqueRepository bloqueRepository;

    @Spy
    private TransaccionPort transaccionPort = new TransaccionDirecta();

    @InjectMocks
    private BloqueService bloqueService;

    @Test
    @DisplayName("Devuelve el bloque del negocio")
    void obtenerShouldReturnBloqueOfNegocio() {
        Bloque bloque = Bloque.builder().id(BLOQUE_ID).negocioId(NEGOCIO_ID).build();
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.of(bloque));

        Bloque result = bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID);

        assertThat(result).isSameAs(bloque);
    }

    @Test
    @DisplayName("Responde 'no existe' cuando el bloque es de otro negocio o no existe")
    void obtenerShouldThrowWhenBloqueIsNotFound() {
        when(bloqueRepository.findByIdAndNegocioId(BLOQUE_ID, NEGOCIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bloqueService.obtener(BLOQUE_ID, NEGOCIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El bloque no existe");
    }

    @Test
    @DisplayName("Crear guarda el bloque que arma el proyecto al final de su lista")
    void crearShouldSaveBloqueBuiltByProyecto() {
        givenSaveReturnsArgument();

        Bloque result = bloqueService.crear(ProyectoTestData.proyecto(), "Clientes", AHORA);

        assertThat(result.getNombre()).isEqualTo("Clientes");
        assertThat(result.getProyectoId()).isEqualTo(ProyectoTestData.PROYECTO_ID);
        assertThat(result.getPosicion()).isEqualTo((short) 2);
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Crear con un nombre repetido en el proyecto no guarda nada")
    void crearShouldNotSaveWhenNameIsTaken() {
        Proyecto proyecto = ProyectoTestData.proyecto();

        assertThatThrownBy(() -> bloqueService.crear(proyecto, "Productos", AHORA))
                .isInstanceOf(DuplicateResourceException.class);
        verify(bloqueRepository, never()).save(any());
    }

    // --- arrange ---
    private void givenSaveReturnsArgument() {
        when(bloqueRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }
}
