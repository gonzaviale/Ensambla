package com.ensambla.backend.domain.useCase.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.EMAIL;
import static com.ensambla.backend.support.UsuarioTestData.NOMBRE;
import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioResponseTest {

    private UsuarioResponse respuesta;

    @Test
    @DisplayName("Devuelve identidad, nombre y email sin exponer el hash")
    void devuelveIdentidadNombreYEmailSinExponerElHash() {
        cuandoSeRespondeConElUsuario();

        entoncesLaRespuestaEsLaIdentidadPublica();
    }

    private void cuandoSeRespondeConElUsuario() {
        respuesta = UsuarioResponse.de(usuario());
    }

    private void entoncesLaRespuestaEsLaIdentidadPublica() {
        assertThat(respuesta).isEqualTo(new UsuarioResponse(usuario().getId(), NOMBRE, EMAIL));
    }
}