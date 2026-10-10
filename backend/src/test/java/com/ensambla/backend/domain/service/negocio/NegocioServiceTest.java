package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
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
class NegocioServiceTest {

    private static final UUID USUARIO_ID = UUID.fromString("b9000000-0000-4000-8000-000000000001");
    private static final UUID NEGOCIO_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @Mock
    private NegocioRepository negocioRepository;

    @InjectMocks
    private NegocioService negocioService;

    @Test
    @DisplayName("Devuelve el id del negocio del que el usuario es dueño")
    void obtenerIdDelUsuarioShouldReturnNegocioId() {
        givenUsuarioHasNegocio();

        UUID result = negocioService.obtenerIdDelUsuario(USUARIO_ID);

        assertThat(result).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Rechaza con 'no existe' al usuario que no tiene negocio")
    void obtenerIdDelUsuarioShouldThrowWhenUsuarioHasNoNegocio() {
        when(negocioRepository.findByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.obtenerIdDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("El negocio no existe");
    }

    // --- arrange ---
    private void givenUsuarioHasNegocio() {
        when(negocioRepository.findByDuenioId(USUARIO_ID))
                .thenReturn(Optional.of(Negocio.builder().id(NEGOCIO_ID).duenioId(USUARIO_ID).build()));
    }
}
