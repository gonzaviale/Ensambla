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
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObtenerSesionTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final ObtenerSesion obtenerSesion = new ObtenerSesion(usuarioRepository);
    private UsuarioResponse sesion;
    private Throwable error;

    @Test
    @DisplayName("Devuelve la identidad del usuario activo de la sesión")
    void devuelveLaIdentidadDelUsuarioActivo() {
        dadoQueElUsuarioEstaActivo();

        cuandoSeObtieneLaSesion();

        entoncesLaSesionEsDeLaPersona();
    }

    @Test
    @DisplayName("Propaga el error del repositorio sin convertirlo")
    void propagaElErrorDelRepositorioSinConvertirlo() {
        dadoQueElUsuarioNoEstaActivo();

        cuandoSeIntentaObtenerLaSesion();

        entoncesSeRechazanLasCredenciales();
    }

    private void dadoQueElUsuarioEstaActivo() {
        when(usuarioRepository.obtenerActivo(USUARIO_ID)).thenReturn(usuario());
    }

    private void dadoQueElUsuarioNoEstaActivo() {
        when(usuarioRepository.obtenerActivo(USUARIO_ID)).thenThrow(new CredencialesInvalidasException());
    }

    private void cuandoSeObtieneLaSesion() {
        sesion = obtenerSesion.execute(USUARIO_ID);
    }

    private void cuandoSeIntentaObtenerLaSesion() {
        error = catchThrowable(this::cuandoSeObtieneLaSesion);
    }

    private void entoncesLaSesionEsDeLaPersona() {
        assertThat(sesion).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
    }

    private void entoncesSeRechazanLasCredenciales() {
        assertThat(error).isInstanceOf(CredencialesInvalidasException.class);
    }
}