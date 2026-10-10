package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
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
    @DisplayName("Resuelve el negocio usando el usuario autenticado")
    void obtenerDelUsuarioShouldResolveOwner() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));

        UUID result = negocioService.obtenerDelUsuario(USUARIO_ID);

        assertThat(result).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Responde no encontrado si el usuario no tiene negocio")
    void obtenerDelUsuarioShouldRejectMissingNegocio() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.obtenerDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
