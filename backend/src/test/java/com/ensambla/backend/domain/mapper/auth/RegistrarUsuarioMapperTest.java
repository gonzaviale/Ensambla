package com.ensambla.backend.domain.mapper.auth;

import com.ensambla.backend.domain.dto.auth.UsuarioResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.UsuarioTestData.usuario;
import static org.assertj.core.api.Assertions.assertThat;

class RegistrarUsuarioMapperTest {

    @Test
    @DisplayName("Devuelve identidad, nombre y email sin exponer el hash")
    void toResponseShouldMapPublicIdentity() {
        RegistrarUsuarioMapper mapper = new RegistrarUsuarioMapper();

        UsuarioResponse result = mapper.toResponse(usuario());

        assertThat(result.id()).isEqualTo(usuario().getId());
        assertThat(result.nombre()).isEqualTo(usuario().getNombre());
        assertThat(result.email()).isEqualTo(usuario().getEmail());
    }
}

