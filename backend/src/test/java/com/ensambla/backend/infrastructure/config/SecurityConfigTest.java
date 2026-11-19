package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.infrastructure.security.UsuarioActivoFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {

    private static final String CONTRASENIA = "Contraseña de prueba";
    private static final String PREFIJO_BCRYPT = "{bcrypt}";

    private final SecurityConfig securityConfig = new SecurityConfig();
    private final PasswordEncoder encoder = securityConfig.passwordEncoder();
    private String hash;

    @Test
    @DisplayName("El encoder guarda la contraseña en formato delegable con BCrypt")
    void elEncoderGuardaEnFormatoDelegable() {
        cuandoSeGuardaLaContrasenia();

        entoncesElHashEmpiezaCon(PREFIJO_BCRYPT);
    }

    @Test
    @DisplayName("El encoder verifica hashes BCrypt anteriores, sin el prefijo")
    void elEncoderVerificaHashesAnteriores() {
        cuandoSeGuardaLaContrasenia();

        entoncesCoincideConElHashSinPrefijo();
    }

    @Test
    @DisplayName("El filtro de identidad se registra solo en Spring Security y no se ejecuta dos veces")
    void elFiltroDeIdentidadSeRegistraSoloEnSpringSecurity() {
        entoncesElRegistroDelFiltroEstaDesactivado(mock(UsuarioActivoFilter.class));
    }

    private void cuandoSeGuardaLaContrasenia() {
        hash = encoder.encode(CONTRASENIA);
    }

    private void entoncesElHashEmpiezaCon(String prefijo) {
        assertThat(hash).startsWith(prefijo);
    }

    private void entoncesCoincideConElHashSinPrefijo() {
        assertThat(encoder.matches(CONTRASENIA, hash.substring(PREFIJO_BCRYPT.length()))).isTrue();
    }

    private void entoncesElRegistroDelFiltroEstaDesactivado(UsuarioActivoFilter filtro) {
        assertThat(securityConfig.usuarioActivoFilterRegistration(filtro).isEnabled()).isFalse();
    }
}