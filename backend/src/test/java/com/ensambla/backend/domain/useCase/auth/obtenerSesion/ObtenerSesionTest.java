package com.ensambla.backend.domain.useCase.auth.obtenerSesion;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import com.ensambla.backend.domain.useCase.auth.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObtenerSesionTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ObtenerSesion useCase = new ObtenerSesion(usuarioRepository);

    @Test
    @DisplayName("Delega al repositorio y devuelve la identidad mapeada")
    void executeShouldDelegateAndMapResponse() {
        when(usuarioRepository.obtenerActivo(USUARIO_ID)).thenReturn(usuario());

        UsuarioResponse result = ejecutar();

        assertThat(result.id()).isEqualTo(usuario().getId());
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.email()).isEqualTo(EMAIL);
        verify(usuarioRepository).obtenerActivo(USUARIO_ID);
    }

    @Test
    @DisplayName("Propaga el error del repositorio sin convertirlo")
    void executeShouldPropagateRepositoryError() {
        when(usuarioRepository.obtenerActivo(USUARIO_ID)).thenThrow(new CredencialesInvalidasException());

        assertThatThrownBy(this::ejecutar).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- act ---
    private UsuarioResponse ejecutar() {
        return useCase.execute(USUARIO_ID);
    }
}

