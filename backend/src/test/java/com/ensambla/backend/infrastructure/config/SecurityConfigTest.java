package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.infrastructure.security.UsuarioActivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("El encoder permite formato delegable y verificación de hashes BCrypt anteriores")
    void passwordEncoderShouldSupportDelegationAndLegacyBcrypt() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();
        String hash = encoder.encode("Contraseña de prueba");

        thenPasswordEncoderShouldSupportDelegationAndLegacyBcrypt(encoder, hash);
    }

    @Test
    @DisplayName("El filtro de identidad se registra solo en Spring Security y no se ejecuta dos veces")
    void usuarioActivoFilterRegistrationShouldDisableServletRegistration() {
        UsuarioActivoFilter filter = mock(UsuarioActivoFilter.class);

        thenUsuarioActivoFilterRegistrationShouldDisableServletRegistration(filter);
    }

    // --- assert ---
    private void thenPasswordEncoderShouldSupportDelegationAndLegacyBcrypt(PasswordEncoder encoder, String hash) {
        assertThat(hash).startsWith("{bcrypt}");
        assertThat(encoder.matches("Contraseña de prueba", hash.substring("{bcrypt}".length()))).isTrue();
    }

    private void thenUsuarioActivoFilterRegistrationShouldDisableServletRegistration(UsuarioActivoFilter filter) {
        assertThat(securityConfig.usuarioActivoFilterRegistration(filter).isEnabled()).isFalse();
    }
}
