package com.ensambla.backend.domain.service.usuario;

import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.model.enums.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.repository.usuario.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Devuelve el usuario cuando existe y está activo")
    void obtenerActivoShouldReturnUsuarioWhenActive() {
        givenRepositoryHas(usuario());

        Usuario result = obtenerActivo();

        assertThat(result.getId()).isEqualTo(USUARIO_ID);
    }

    @Test
    @DisplayName("Rechaza al usuario suspendido con credenciales inválidas")
    void obtenerActivoShouldThrowWhenSuspended() {
        givenRepositoryHas(Usuario.builder().id(USUARIO_ID).estado(EstadoUsuario.SUSPENDIDO).build());

        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    @DisplayName("Rechaza al usuario inexistente con credenciales inválidas")
    void obtenerActivoShouldThrowWhenMissing() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(this::obtenerActivo).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- arrange ---
    private void givenRepositoryHas(Usuario usuario) {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario));
    }

    // --- act ---
    private Usuario obtenerActivo() {
        return usuarioService.obtenerActivo(USUARIO_ID);
    }
}
