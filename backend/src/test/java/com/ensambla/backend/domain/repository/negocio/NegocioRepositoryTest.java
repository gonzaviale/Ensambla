package com.ensambla.backend.domain.repository.negocio;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.Negocio;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.ensambla.backend.support.ProyectoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ProyectoTestData.USUARIO_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class NegocioRepositoryTest {

    private final NegocioRepository negocioRepository = mock(NegocioRepository.class, CALLS_REAL_METHODS);

    @Test
    @DisplayName("Resuelve el negocio usando el usuario autenticado")
    void obtenerIdDelUsuarioShouldResolveOwner() {
        givenIdDelNegocio(Optional.of(NEGOCIO_ID));

        assertThat(negocioRepository.obtenerIdDelUsuario(USUARIO_ID)).isEqualTo(NEGOCIO_ID);
    }

    @Test
    @DisplayName("Responde no encontrado si el usuario no tiene negocio")
    void obtenerIdDelUsuarioShouldRejectMissingNegocio() {
        givenIdDelNegocio(Optional.empty());

        assertThatThrownBy(() -> negocioRepository.obtenerIdDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el negocio del usuario");
    }

    @Test
    @DisplayName("Devuelve el negocio del usuario")
    void obtenerNegocioDelUsuarioShouldReturnOwnNegocio() {
        Negocio negocio = Negocio.builder().id(NEGOCIO_ID).build();
        givenNegocio(Optional.of(negocio));

        assertThat(negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID)).isSameAs(negocio);
    }

    @Test
    @DisplayName("Responde no encontrado al pedir el negocio de un usuario sin negocio")
    void obtenerNegocioDelUsuarioShouldRejectMissingNegocio() {
        givenNegocio(Optional.empty());

        assertThatThrownBy(() -> negocioRepository.obtenerNegocioDelUsuario(USUARIO_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró el negocio del usuario");
    }

    // --- arrange ---
    private void givenIdDelNegocio(Optional<NegocioId> id) {
        doReturn(id).when(negocioRepository).findIdByDuenioId(USUARIO_ID);
    }

    private void givenNegocio(Optional<Negocio> negocio) {
        doReturn(negocio).when(negocioRepository).findByDuenioId(USUARIO_ID);
    }
}
