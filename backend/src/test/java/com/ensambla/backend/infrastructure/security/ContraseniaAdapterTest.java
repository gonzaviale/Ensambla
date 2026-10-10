package com.ensambla.backend.infrastructure.security;

import com.ensambla.backend.infrastructure.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContraseniaAdapterTest {

    private final ContraseniaAdapter contraseniaAdapter =
            new ContraseniaAdapter(new SecurityConfig().passwordEncoder());

    @Test
    @DisplayName("BCrypt produce un hash con sal verificable que no contiene la contraseña")
    void hashearShouldStoreSaltedBcryptHash() {
        String contrasenia = "ClaveSegura123!";

        String hash = contraseniaAdapter.hashear(contrasenia);

        assertThat(hash).startsWith("{bcrypt}");
        assertThat(hash).doesNotContain(contrasenia);
        assertThat(hash).isNotEqualTo(contraseniaAdapter.hashear(contrasenia));
        assertThat(contraseniaAdapter.coincide(contrasenia, hash)).isTrue();
        assertThat(contraseniaAdapter.coincide("incorrecta", hash)).isFalse();
    }
}

