package com.ensambla.backend.domain.service.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.repository.negocio.NegocioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NegocioServiceTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class);
    private final NegocioService negocioService = new NegocioService(negocioRepository);

    @Test
    @DisplayName("Resuelve el negocio usando el usuario autenticado")
    void obtenerDelUsuarioShouldResolveOwner() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.of(NEGOCIO_ID));

        var result = negocioService.obtenerDelUsuario(USUARIO_ID);

        assertThat(result).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Lista los ids de todos los negocios")
    void listarIdsShouldReturnEveryNegocioId() {
        when(negocioRepository.findAllIds()).thenReturn(List.of(NEGOCIO_ID));

        assertThat(negocioService.listarIds()).containsExactly(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Responde no encontrado si el usuario no tiene negocio")
    void obtenerDelUsuarioShouldRejectMissingNegocio() {
        when(negocioRepository.findIdByDuenioId(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> negocioService.obtenerDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

