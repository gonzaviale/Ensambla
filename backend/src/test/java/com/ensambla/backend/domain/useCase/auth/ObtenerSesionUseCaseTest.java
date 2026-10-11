package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.mapper.auth.ObtenerSesionMapper;
import com.ensambla.backend.domain.service.usuario.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.USUARIO_ID;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ObtenerSesionUseCaseTest {

    private final UsuarioService service = mock(UsuarioService.class);
    private final ObtenerSesionUseCase useCase = new ObtenerSesionUseCase(
            service, new ObtenerSesionMapper());

    @Test
    @DisplayName("Delega al servicio y devuelve la identidad mapeada")
    void executeShouldDelegateAndMapResponse() {
        when(service.obtenerActivo(USUARIO_ID)).thenReturn(usuario());

        UsuarioResponse result = ejecutar();

        assertThat(result.id()).isEqualTo(usuario().getId());
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.email()).isEqualTo(EMAIL);
        verify(service).obtenerActivo(USUARIO_ID);
    }

    @Test
    @DisplayName("Propaga el error del servicio sin convertirlo")
    void executeShouldPropagateServiceError() {
        when(service.obtenerActivo(USUARIO_ID)).thenThrow(new CredencialesInvalidasException());

        assertThatThrownBy(this::ejecutar).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- act ---
    private UsuarioResponse ejecutar() {
        return useCase.execute(USUARIO_ID);
    }
}

