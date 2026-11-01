package com.ensambla.backend.domain.useCase.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class UsuarioResponseTest {

    @Test
    @DisplayName("Devuelve identidad, nombre y email sin exponer el hash")
    void deShouldMapPublicIdentity() {
        UsuarioResponse result = UsuarioResponse.de(usuario());

        assertThat(result.id()).isEqualTo(usuario().getId());
        assertThat(result.nombre()).isEqualTo(usuario().getNombre());
        assertThat(result.email()).isEqualTo(usuario().getEmail());
    }
}

