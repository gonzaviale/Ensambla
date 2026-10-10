package com.ensambla.backend.domain.useCase.auth;

import com.ensambla.backend.domain.dto.auth.RegistrarUsuarioRequest;
import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import com.ensambla.backend.domain.exception.CredencialesInvalidasException;
import com.ensambla.backend.domain.mapper.auth.RegistrarUsuarioMapper;
import com.ensambla.backend.domain.service.auth.RegistroService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.ZoneOffset;

import static com.ensambla.backend.support.UsuarioTestData.CONTRASENIA;
import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.FECHA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RegistrarUsuarioUseCaseTest {

    private final RegistroService service = mock(RegistroService.class);
    private final RegistrarUsuarioUseCase useCase = new RegistrarUsuarioUseCase(
            service, new RegistrarUsuarioMapper(), Clock.fixed(FECHA.toInstant(), ZoneOffset.UTC));

    @Test
    @DisplayName("Delega al servicio y devuelve la identidad mapeada")
    void executeShouldDelegateAndMapResponse() {
        when(service.registrar(any(), eq(FECHA))).thenReturn(usuario());

        UsuarioResponse result = ejecutar();

        assertThat(result.id()).isEqualTo(usuario().getId());
        assertThat(result.nombre()).isEqualTo(NOMBRE);
        assertThat(result.email()).isEqualTo(EMAIL);
        verify(service).registrar(any(), eq(FECHA));
    }

    @Test
    @DisplayName("Propaga el error del servicio sin convertirlo")
    void executeShouldPropagateServiceError() {
        when(service.registrar(any(), eq(FECHA))).thenThrow(new CredencialesInvalidasException());

        assertThatThrownBy(this::ejecutar).isInstanceOf(CredencialesInvalidasException.class);
    }

    // --- act ---
    private UsuarioResponse ejecutar() {
        return useCase.execute(new RegistrarUsuarioRequest(NOMBRE, "Negocio", EMAIL, CONTRASENIA));
    }
}

