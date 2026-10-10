package com.ensambla.backend.domain.service.bloque;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.repository.bloque.BloqueRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueServiceTest {

    private static final UUID BLOQUE_ID = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @Mock
    private BloqueRepository bloqueRepository;

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
}
